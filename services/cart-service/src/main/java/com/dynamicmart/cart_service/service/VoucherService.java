package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;
import static com.dynamicmart.cart_service.dto.VoucherDtos.*;
import static com.dynamicmart.cart_service.dto.PromotionDtos.PromotionAuditResponse;

import com.dynamicmart.cart_service.entity.CustomerVoucher;
import com.dynamicmart.cart_service.entity.PromotionAudit;
import com.dynamicmart.cart_service.entity.Voucher;
import com.dynamicmart.cart_service.entity.VoucherReservation;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CustomerVoucherRepository;
import com.dynamicmart.cart_service.repository.PromotionAuditRepository;
import com.dynamicmart.cart_service.repository.VoucherRepository;
import com.dynamicmart.cart_service.repository.VoucherReservationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {
    private static final Set<String> SCOPES = Set.of("ORDER_DISCOUNT", "SHIPPING_DISCOUNT", "PRODUCT_DISCOUNT", "CATEGORY_DISCOUNT", "PRODUCT_LIST_DISCOUNT");
    private static final Set<String> METHODS = Set.of("FIXED_AMOUNT", "PERCENTAGE");
    private static final Set<String> MODES = Set.of("DEFAULT_FOR_ELIGIBLE", "ASSIGNED_ONLY", "CODE_ONLY");
    private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "DISABLED");
    private final VoucherRepository vouchers;
    private final CustomerVoucherRepository customerVouchers;
    private final VoucherReservationRepository reservations;
    private final PromotionAuditRepository audits;

    public VoucherService(VoucherRepository vouchers, CustomerVoucherRepository customerVouchers,
                          VoucherReservationRepository reservations, PromotionAuditRepository audits) {
        this.vouchers = vouchers; this.customerVouchers = customerVouchers; this.reservations = reservations; this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> adminList() { return vouchers.findAll().stream().map(v -> response(v, null, true, null, 0)).toList(); }

    @Transactional(readOnly = true)
    public List<PromotionAuditResponse> audits(UUID id) {
        require(id); return audits.findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc("VOUCHER", id).stream()
                .map(a -> new PromotionAuditResponse(a.getId(), a.getActorAdminId(), a.getTargetType(), a.getTargetId(), a.getAction(), a.getReason(), a.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> reservationHistory() {
        return reservations.findAllByOrderByCreatedAtDesc().stream().map(this::reservation).toList();
    }

    @Transactional
    public VoucherResponse create(UUID actor, UUID key, VoucherRuleRequest request) {
        var duplicate = duplicate(actor, key, "VOUCHER");
        if (duplicate != null) return response(require(duplicate), null, true, null, 0);
        validateRule(request, true); Instant now = Instant.now();
        String code = normalize(request.code());
        if (vouchers.findByCodeIgnoreCase(code).isPresent()) throw conflict("VOUCHER_CODE_EXISTS", "Mã voucher đã tồn tại.");
        Voucher voucher = vouchers.save(new Voucher(code, request.name().trim(), request.description(), request.scope(),
                request.discountMethod(), request.fixedDiscountVnd(), request.discountRateBps(), request.maxDiscountVnd(),
                request.minimumOrderVnd(), request.minimumEligibleSubtotalVnd(), request.usageLimit(),
                request.usageLimitPerCustomer(), request.startsAt(), request.endsAt(), request.distributionMode(),
                request.defaultVoucher(), safe(request.productIds()), safe(request.categoryIds()), actor, now));
        audit(actor, "VOUCHER", voucher.getId(), "CREATE", request.reason(), key, now);
        return response(voucher, null, true, null, 0);
    }

    @Transactional
    public VoucherResponse update(UUID actor, UUID id, UUID key, VoucherRuleRequest request) {
        var duplicate = duplicate(actor, key, "VOUCHER");
        if (duplicate != null) return response(require(duplicate), null, true, null, 0);
        Voucher voucher = require(id); validateRule(request, false);
        if (!voucher.getCode().equalsIgnoreCase(normalize(request.code())))
            throw invalid("VOUCHER_CODE_IMMUTABLE", "Không được đổi code của voucher sau khi tạo.");
        voucher.update(request.name().trim(), request.description(), request.scope(), request.discountMethod(),
                request.fixedDiscountVnd(), request.discountRateBps(), request.maxDiscountVnd(), request.minimumOrderVnd(),
                request.minimumEligibleSubtotalVnd(), request.usageLimit(), request.usageLimitPerCustomer(),
                request.startsAt(), request.endsAt(), request.distributionMode(), request.defaultVoucher(),
                safe(request.productIds()), safe(request.categoryIds()), Instant.now());
        audit(actor, "VOUCHER", id, "UPDATE", request.reason(), key, Instant.now());
        return response(voucher, null, true, null, 0);
    }

    @Transactional
    public VoucherResponse changeStatus(UUID actor, UUID id, UUID key, VoucherStatusRequest request) {
        var duplicate = duplicate(actor, key, "VOUCHER");
        if (duplicate != null) return response(require(duplicate), null, true, null, 0);
        if (!STATUSES.contains(request.status())) throw invalid("VOUCHER_STATUS_INVALID", "Trạng thái voucher không hợp lệ.");
        Voucher voucher = require(id);
        if ("ACTIVE".equals(request.status()) && !Instant.now().isBefore(voucher.getEndsAt()))
            throw invalid("VOUCHER_EXPIRED", "Không thể bật voucher đã hết hạn.");
        voucher.changeStatus(request.status(), Instant.now());
        audit(actor, "VOUCHER", id, "STATUS_" + request.status(), request.reason(), key, Instant.now());
        return response(voucher, null, true, null, 0);
    }

    @Transactional
    public AssignmentResponse assign(UUID actor, UUID voucherId, UUID key, AssignmentRequest request) {
        var duplicate = duplicate(actor, key, "CUSTOMER_VOUCHER");
        if (duplicate != null) return assignment(requireCustomerVoucher(duplicate));
        Voucher voucher = require(voucherId);
        if (!"ASSIGNED_ONLY".equals(voucher.getDistributionMode())) throw invalid("VOUCHER_NOT_ASSIGNABLE", "Chỉ voucher ASSIGNED_ONLY mới được cấp vào ví.");
        Instant now = Instant.now();
        if (request.expiresAt() != null && (!request.expiresAt().isAfter(now) || request.expiresAt().isAfter(voucher.getEndsAt())))
            throw invalid("ASSIGNMENT_EXPIRY_INVALID", "Hạn của voucher trong ví phải còn hiệu lực và không vượt hạn voucher.");
        CustomerVoucher assigned = customerVouchers.findByCustomerIdAndVoucherId(request.customerId(), voucherId)
                .map(value -> { value.makeAvailable(request.expiresAt(), actor, now); return value; })
                .orElseGet(() -> customerVouchers.save(new CustomerVoucher(request.customerId(), voucherId, request.expiresAt(), actor, now)));
        audit(actor, "CUSTOMER_VOUCHER", assigned.getId(), "ASSIGN", request.reason(), key, now);
        return assignment(assigned);
    }

    @Transactional
    public AssignmentResponse revoke(UUID actor, UUID assignmentId, UUID key, String reason) {
        var duplicate = duplicate(actor, key, "CUSTOMER_VOUCHER");
        if (duplicate != null) return assignment(requireCustomerVoucher(duplicate));
        if (reason == null || reason.isBlank() || reason.length() > 500) throw invalid("REASON_INVALID", "Cần nhập lý do hợp lệ.");
        CustomerVoucher assignment = requireCustomerVoucher(assignmentId); assignment.revoke();
        audit(actor, "CUSTOMER_VOUCHER", assignmentId, "REVOKE", reason, key, Instant.now());
        return assignment(assignment);
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> wallet(UUID customerId) {
        Instant now = Instant.now(); List<VoucherResponse> result = new ArrayList<>(); Set<UUID> seen = new HashSet<>();
        for (CustomerVoucher cv : customerVouchers.findAllByCustomerIdOrderByAssignedAtDesc(customerId)) {
            vouchers.findById(cv.getVoucherId()).ifPresent(v -> {
                boolean eligible = cv.availableAt(now) && v.activeAt(now);
                result.add(response(v, cv.getId(), eligible, eligible ? null : "Voucher đã hết hạn, đã dùng hoặc bị thu hồi.", 0)); seen.add(v.getId());
            });
        }
        for (Voucher v : vouchers.findVisibleDefaults(now)) if (seen.add(v.getId())) result.add(response(v, null, true, null, 0));
        return result;
    }

    @Transactional(readOnly = true)
    public VoucherResponse preview(UUID customerId, VoucherPreviewRequest request) {
        Voucher voucher = locate(request.voucherId(), request.code());
        Evaluation evaluation = evaluate(voucher, customerId, request.code(), request.orderSubtotalVnd(),
                request.eligibleSubtotalVnd(), request.shippingFeeVnd(), safe(request.productIds()), safe(request.categoryIds()), false);
        return response(voucher, evaluation.customerVoucherId(), evaluation.eligible(), evaluation.reason(), evaluation.discount());
    }

    @Transactional
    public ReservationResponse reserve(ReserveVoucherRequest request) {
        require(request.voucherId());
        var existing = reservations.findByCheckoutSessionIdAndVoucherId(request.checkoutSessionId(), request.voucherId());
        if (existing.isPresent()) {
            if (!existing.get().getCustomerId().equals(request.customerId())) throw conflict("VOUCHER_RESERVATION_OWNERSHIP", "Phiên đặt hàng không thuộc khách hàng này.");
            return reservation(existing.get());
        }
        Voucher voucher = vouchers.findByIdForUpdate(request.voucherId()).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND", "Không tìm thấy voucher."));
        Evaluation evaluation = evaluate(voucher, request.customerId(), voucher.getCode(), request.orderSubtotalVnd(),
                request.eligibleSubtotalVnd(), request.shippingFeeVnd(), safe(request.productIds()), safe(request.categoryIds()), true);
        if (!evaluation.eligible()) throw invalid("VOUCHER_INELIGIBLE", evaluation.reason());
        if (voucher.getUsageLimit() != null && reservations.countAllocated(voucher.getId()) >= voucher.getUsageLimit())
            throw conflict("VOUCHER_QUOTA_EXHAUSTED", "Voucher đã hết lượt sử dụng.");
        if (voucher.getUsageLimitPerCustomer() != null && reservations.countByVoucherIdAndCustomerIdAndStatusIn(
                voucher.getId(), request.customerId(), List.of("RESERVED", "CONSUMED")) >= voucher.getUsageLimitPerCustomer())
            throw conflict("VOUCHER_CUSTOMER_LIMIT", "Bạn đã dùng hết lượt của voucher này.");
        boolean shipping = "SHIPPING_DISCOUNT".equals(voucher.getScope());
        if (reservations.countSlot(request.checkoutSessionId(), shipping) > 0)
            throw conflict("VOUCHER_STACKING_NOT_ALLOWED", shipping ? "Chỉ được dùng một voucher phí giao hàng." : "Chỉ được dùng một voucher hàng hóa.");
        Instant until = request.reservedUntil().isBefore(voucher.getEndsAt()) ? request.reservedUntil() : voucher.getEndsAt();
        long merchandise = shipping ? 0 : evaluation.discount(); long shippingDiscount = shipping ? evaluation.discount() : 0;
        return reservation(reservations.save(new VoucherReservation(voucher.getId(), request.customerId(),
                evaluation.customerVoucherId(), request.checkoutSessionId(), merchandise, shippingDiscount, until, Instant.now())));
    }

    @Transactional
    public ReservationResponse consume(UUID reservationId, UUID orderId) {
        VoucherReservation reservation = requireReservation(reservationId);
        if ("CONSUMED".equals(reservation.getStatus())) {
            if (!orderId.equals(reservation.getOrderId())) throw conflict("VOUCHER_RESERVATION_ORDER_MISMATCH", "Lượt voucher đã được dùng cho đơn hàng khác.");
            return reservation(reservation);
        }
        if (!"RESERVED".equals(reservation.getStatus())) throw conflict("VOUCHER_RESERVATION_NOT_ACTIVE", "Lượt voucher không còn ở trạng thái giữ.");
        if (!Instant.now().isBefore(reservation.getReservedUntil())) throw conflict("VOUCHER_RESERVATION_EXPIRED", "Lượt giữ voucher đã hết hạn.");
        Voucher voucher = vouchers.findByIdForUpdate(reservation.getVoucherId()).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND", "Không tìm thấy voucher."));
        reservation.consume(orderId, Instant.now()); voucher.consume();
        if (reservation.getCustomerVoucherId() != null) customerVouchers.findByIdAndCustomerIdAndVoucherId(
                reservation.getCustomerVoucherId(), reservation.getCustomerId(), reservation.getVoucherId()).ifPresent(CustomerVoucher::useUp);
        return reservation(reservation);
    }

    @Transactional
    public ReservationResponse release(UUID reservationId, String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 80) throw invalid("RELEASE_REASON_INVALID", "Lý do trả lượt không hợp lệ.");
        VoucherReservation reservation = requireReservation(reservationId);
        if ("RELEASED".equals(reservation.getStatus())) return reservation(reservation);
        if ("CONSUMED".equals(reservation.getStatus())) throw conflict("VOUCHER_ALREADY_CONSUMED", "Voucher đã dùng không thể trả lượt.");
        reservation.release(reason.trim(), Instant.now()); return reservation(reservation);
    }

    private Evaluation evaluate(Voucher v, UUID customerId, String suppliedCode, long orderSubtotal, long eligibleSubtotal,
                                long shippingFee, Set<UUID> productIds, Set<UUID> categoryIds, boolean reserve) {
        Instant now = Instant.now(); UUID assignmentId = null;
        if (!v.activeAt(now)) return Evaluation.no("Voucher chưa hoạt động hoặc đã hết hạn.");
        if (v.getUsageLimit() != null && reservations.countAllocated(v.getId()) >= v.getUsageLimit()) return Evaluation.no("Voucher đã hết lượt sử dụng.");
        if (v.getUsageLimitPerCustomer() != null && reservations.countByVoucherIdAndCustomerIdAndStatusIn(v.getId(), customerId, List.of("RESERVED", "CONSUMED")) >= v.getUsageLimitPerCustomer()) return Evaluation.no("Bạn đã dùng hết lượt của voucher này.");
        if ("CODE_ONLY".equals(v.getDistributionMode()) && (suppliedCode == null || !v.getCode().equals(normalize(suppliedCode)))) return Evaluation.no("Cần nhập đúng mã voucher.");
        if ("ASSIGNED_ONLY".equals(v.getDistributionMode())) {
            CustomerVoucher cv = customerVouchers.findByCustomerIdAndVoucherId(customerId, v.getId()).orElse(null);
            if (cv == null || !cv.availableAt(now)) return Evaluation.no("Voucher không có trong ví hoặc đã hết hiệu lực.");
            assignmentId = cv.getId();
        }
        if (v.getMinimumOrderVnd() != null && orderSubtotal < v.getMinimumOrderVnd()) return Evaluation.no("Chưa đạt giá trị đơn hàng tối thiểu.");
        if (v.getMinimumEligibleSubtotalVnd() != null && eligibleSubtotal < v.getMinimumEligibleSubtotalVnd()) return Evaluation.no("Chưa đạt giá trị hàng đủ điều kiện tối thiểu.");
        if (("PRODUCT_DISCOUNT".equals(v.getScope()) || "PRODUCT_LIST_DISCOUNT".equals(v.getScope())) && disjoint(v.getProductIds(), productIds)) return Evaluation.no("Không có sản phẩm phù hợp voucher.");
        if ("CATEGORY_DISCOUNT".equals(v.getScope()) && disjoint(v.getCategoryIds(), categoryIds)) return Evaluation.no("Không có danh mục phù hợp voucher.");
        long base = switch (v.getScope()) { case "SHIPPING_DISCOUNT" -> shippingFee; case "ORDER_DISCOUNT" -> orderSubtotal; default -> eligibleSubtotal; };
        long discount = v.discount(base);
        if (discount <= 0) return Evaluation.no("Không có giá trị đủ điều kiện để giảm.");
        return new Evaluation(true, null, discount, assignmentId);
    }

    private void validateRule(VoucherRuleRequest r, boolean creating) {
        if (!SCOPES.contains(r.scope()) || !METHODS.contains(r.discountMethod()) || !MODES.contains(r.distributionMode())) throw invalid("VOUCHER_RULE_INVALID", "Loại, cách giảm hoặc cách phân phối voucher không hợp lệ.");
        if (!r.endsAt().isAfter(r.startsAt())) throw invalid("VOUCHER_TIME_INVALID", "Thời gian kết thúc phải sau thời gian bắt đầu.");
        boolean fixed = "FIXED_AMOUNT".equals(r.discountMethod());
        if ((fixed && (r.fixedDiscountVnd() == null || r.discountRateBps() != null)) || (!fixed && (r.discountRateBps() == null || r.fixedDiscountVnd() != null || r.discountRateBps() > 10_000))) throw invalid("DISCOUNT_VALUE_INVALID", "Giá trị giảm không khớp cách giảm.");
        if (r.defaultVoucher() && !"DEFAULT_FOR_ELIGIBLE".equals(r.distributionMode())) throw invalid("VOUCHER_DEFAULT_INVALID", "Voucher mặc định phải dùng DEFAULT_FOR_ELIGIBLE.");
        if (r.usageLimit() != null && !creating && r.usageLimit() < 1) throw invalid("VOUCHER_USAGE_LIMIT_INVALID", "Giới hạn lượt dùng không hợp lệ.");
        if (("PRODUCT_DISCOUNT".equals(r.scope()) || "PRODUCT_LIST_DISCOUNT".equals(r.scope())) && safe(r.productIds()).isEmpty()) throw invalid("VOUCHER_PRODUCTS_REQUIRED", "Voucher sản phẩm cần ít nhất một Product.");
        if ("CATEGORY_DISCOUNT".equals(r.scope()) && safe(r.categoryIds()).isEmpty()) throw invalid("VOUCHER_CATEGORIES_REQUIRED", "Voucher danh mục cần ít nhất một Category.");
    }
    private UUID duplicate(UUID actor, UUID key, String expectedType) {
        var found = audits.findByActorAdminIdAndIdempotencyKey(actor, key).orElse(null);
        if (found == null) return null;
        if (!expectedType.equals(found.getTargetType())) throw conflict("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key đã được dùng cho thao tác khác.");
        return found.getTargetId();
    }
    private void audit(UUID actor, String type, UUID target, String action, String reason, UUID key, Instant now) { audits.save(new PromotionAudit(actor, type, target, action, reason.trim(), key, now)); }
    private Voucher locate(UUID id, String code) { if (id != null) return require(id); if (code != null && !code.isBlank()) return vouchers.findByCodeIgnoreCase(normalize(code)).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND", "Không tìm thấy voucher.")); throw invalid("VOUCHER_IDENTIFIER_REQUIRED", "Cần voucherId hoặc code."); }
    private Voucher require(UUID id) { return vouchers.findById(id).orElseThrow(() -> notFound("VOUCHER_NOT_FOUND", "Không tìm thấy voucher.")); }
    private CustomerVoucher requireCustomerVoucher(UUID id) { return customerVouchers.findById(id).orElseThrow(() -> notFound("CUSTOMER_VOUCHER_NOT_FOUND", "Không tìm thấy voucher trong ví.")); }
    private VoucherReservation requireReservation(UUID id) { return reservations.findByIdForUpdate(id).orElseThrow(() -> notFound("VOUCHER_RESERVATION_NOT_FOUND", "Không tìm thấy lượt giữ voucher.")); }
    private VoucherResponse response(Voucher v, UUID cv, boolean eligible, String reason, long discount) { return new VoucherResponse(v.getId(), v.getCode(), v.getName(), v.getDescription(), v.getStatus(), v.getScope(), v.getDiscountMethod(), v.getFixedDiscountVnd(), v.getDiscountRateBps(), v.getMaxDiscountVnd(), v.getMinimumOrderVnd(), v.getMinimumEligibleSubtotalVnd(), v.getUsageLimit(), v.getConsumedCount(), v.getUsageLimitPerCustomer(), v.getStartsAt(), v.getEndsAt(), v.getDistributionMode(), v.isDefaultVoucher(), Set.copyOf(v.getProductIds()), Set.copyOf(v.getCategoryIds()), eligible, reason, discount, cv); }
    private AssignmentResponse assignment(CustomerVoucher cv) { return new AssignmentResponse(cv.getId(), cv.getCustomerId(), cv.getVoucherId(), cv.getStatus(), cv.getAssignedAt(), cv.getExpiresAt()); }
    private ReservationResponse reservation(VoucherReservation r) { return new ReservationResponse(r.getId(), r.getVoucherId(), r.getCustomerId(), r.getCheckoutSessionId(), r.getOrderId(), r.getStatus(), r.getDiscountAmountVnd(), r.getShippingDiscountVnd(), r.getReservedUntil()); }
    private static String normalize(String code) { return code.trim().toUpperCase(Locale.ROOT); }
    private static Set<UUID> safe(Set<UUID> values) { return values == null ? Set.of() : Set.copyOf(values); }
    private static boolean disjoint(Set<UUID> configured, Set<UUID> actual) { return configured.isEmpty() || java.util.Collections.disjoint(configured, actual); }
    private CartException invalid(String code, String message) { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
    private CartException conflict(String code, String message) { return new CartException(HttpStatus.CONFLICT, code, message); }
    private CartException notFound(String code, String message) { return new CartException(HttpStatus.NOT_FOUND, code, message); }
    private record Evaluation(boolean eligible, String reason, long discount, UUID customerVoucherId) { static Evaluation no(String reason) { return new Evaluation(false, reason, 0, null); } }
}
