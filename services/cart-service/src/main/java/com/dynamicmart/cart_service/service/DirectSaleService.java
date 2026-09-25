package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.PromotionDtos.*;

import com.dynamicmart.cart_service.entity.DirectPricePromotion;
import com.dynamicmart.cart_service.entity.PromotionAudit;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.DirectPricePromotionRepository;
import com.dynamicmart.cart_service.repository.PromotionAuditRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectSaleService {
    private static final Set<String> METHODS = Set.of("FIXED_AMOUNT", "PERCENTAGE");
    private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "PAUSED", "ARCHIVED");
    private final DirectPricePromotionRepository promotions;
    private final PromotionAuditRepository audits;
    public DirectSaleService(DirectPricePromotionRepository promotions, PromotionAuditRepository audits) {
        this.promotions = promotions; this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<DirectSaleResponse> list() { return promotions.findAll().stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public DirectSaleResponse get(UUID id) { return response(require(id)); }

    @Transactional(readOnly = true)
    public List<PromotionAuditResponse> audits(UUID id) {
        require(id); return audits.findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc("DIRECT_SALE", id).stream()
                .map(a -> new PromotionAuditResponse(a.getId(), a.getActorAdminId(), a.getTargetType(), a.getTargetId(), a.getAction(), a.getReason(), a.getCreatedAt())).toList();
    }

    @Transactional
    public DirectSaleResponse create(UUID actor, UUID key, DirectSaleRequest request) {
        var duplicate = audits.findByActorAdminIdAndIdempotencyKey(actor, key);
        if (duplicate.isPresent()) return getDuplicate(duplicate.get());
        validate(request);
        Instant now = Instant.now();
        DirectPricePromotion promotion = promotions.save(new DirectPricePromotion(request.name().trim(), request.description(),
                request.discountMethod(), request.fixedDiscountVnd(), request.discountRateBps(), request.maxDiscountVnd(),
                request.startsAt(), request.endsAt(), actor, request.variantIds(), now));
        audits.save(new PromotionAudit(actor, "DIRECT_SALE", promotion.getId(), "CREATE", request.reason().trim(), key, now));
        return response(promotion);
    }

    @Transactional
    public DirectSaleResponse update(UUID actor, UUID id, UUID key, DirectSaleRequest request) {
        var duplicate = audits.findByActorAdminIdAndIdempotencyKey(actor, key);
        if (duplicate.isPresent()) return getDuplicate(duplicate.get());
        validate(request); DirectPricePromotion promotion = require(id);
        if ("ACTIVE".equals(promotion.getStatus())) ensureNoOverlap(id, request.variantIds(), request.startsAt(), request.endsAt());
        promotion.update(request.name().trim(), request.description(), request.discountMethod(), request.fixedDiscountVnd(),
                request.discountRateBps(), request.maxDiscountVnd(), request.startsAt(), request.endsAt(), request.variantIds(), Instant.now());
        audits.save(new PromotionAudit(actor, "DIRECT_SALE", id, "UPDATE", request.reason().trim(), key, Instant.now()));
        return response(promotion);
    }

    @Transactional
    public DirectSaleResponse changeStatus(UUID actor, UUID id, UUID key, StatusRequest request) {
        var duplicate = audits.findByActorAdminIdAndIdempotencyKey(actor, key);
        if (duplicate.isPresent()) return getDuplicate(duplicate.get());
        if (!STATUSES.contains(request.status())) throw invalid("DIRECT_SALE_STATUS_INVALID", "Trạng thái chiến dịch không hợp lệ.");
        DirectPricePromotion promotion = require(id);
        if ("ACTIVE".equals(request.status())) {
            Instant now = Instant.now();
            if (!now.isBefore(promotion.getEndsAt())) throw invalid("DIRECT_SALE_EXPIRED", "Không thể bật chiến dịch đã hết hạn.");
            ensureNoOverlap(id, promotion.getVariantIds(), promotion.getStartsAt(), promotion.getEndsAt());
        }
        promotion.changeStatus(request.status(), Instant.now());
        audits.save(new PromotionAudit(actor, "DIRECT_SALE", id, "STATUS_" + request.status(), request.reason().trim(), key, Instant.now()));
        return response(promotion);
    }

    @Transactional(readOnly = true)
    public DirectSalePriceResponse resolve(UUID variantId, long listPriceVnd) {
        if (listPriceVnd < 0) throw invalid("LIST_PRICE_INVALID", "Giá niêm yết không được âm.");
        List<DirectPricePromotion> active = promotions.findActiveForVariant(variantId, Instant.now());
        if (active.size() > 1) throw new CartException(HttpStatus.CONFLICT, "DIRECT_SALE_OVERLAP", "Variant có nhiều chiến dịch đang hiệu lực.");
        if (active.isEmpty()) return new DirectSalePriceResponse(variantId, listPriceVnd, 0, listPriceVnd, 0, null, null, null);
        DirectPricePromotion p = active.get(0); long discount = p.discount(listPriceVnd);
        int percent = listPriceVnd == 0 ? 0 : (int) Math.min(100, discount * 100 / listPriceVnd);
        return new DirectSalePriceResponse(variantId, listPriceVnd, discount, listPriceVnd - discount, percent, p.getId(), p.getName(), p.getEndsAt());
    }

    private void validate(DirectSaleRequest r) {
        if (!METHODS.contains(r.discountMethod())) throw invalid("DISCOUNT_METHOD_INVALID", "Cách giảm giá không hợp lệ.");
        if (!r.endsAt().isAfter(r.startsAt())) throw invalid("PROMOTION_TIME_INVALID", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        boolean fixed = "FIXED_AMOUNT".equals(r.discountMethod());
        if ((fixed && (r.fixedDiscountVnd() == null || r.discountRateBps() != null)) ||
                (!fixed && (r.discountRateBps() == null || r.fixedDiscountVnd() != null || r.discountRateBps() > 10_000)))
            throw invalid("DISCOUNT_VALUE_INVALID", "Giá trị giảm giá không khớp với cách giảm.");
    }
    private void ensureNoOverlap(UUID id, Set<UUID> variants, Instant start, Instant end) {
        if (!promotions.findOverlapping(variants, id, start, end).isEmpty())
            throw new CartException(HttpStatus.CONFLICT, "DIRECT_SALE_OVERLAP", "Một Variant đã có direct sale trùng thời gian.");
    }
    private DirectPricePromotion require(UUID id) { return promotions.findById(id).orElseThrow(() -> new CartException(HttpStatus.NOT_FOUND, "DIRECT_SALE_NOT_FOUND", "Không tìm thấy chiến dịch.")); }
    private DirectSaleResponse getDuplicate(PromotionAudit audit) {
        if (!"DIRECT_SALE".equals(audit.getTargetType())) throw new CartException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency-Key đã được dùng cho thao tác khác.");
        return get(audit.getTargetId());
    }
    private DirectSaleResponse response(DirectPricePromotion p) { return new DirectSaleResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(), p.getDiscountMethod(), p.getFixedDiscountVnd(), p.getDiscountRateBps(), p.getMaxDiscountVnd(), p.getStartsAt(), p.getEndsAt(), Set.copyOf(p.getVariantIds()), p.getUpdatedAt()); }
    private CartException invalid(String code, String message) { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
}
