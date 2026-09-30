package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.CommitInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.InventoryReservationItemRequest;
import com.dynamicmart.catalog_service.dto.request.ReleaseInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.ReserveInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationItemResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationResponse;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.InventoryOperationType;
import com.dynamicmart.catalog_service.entity.InventoryReservation;
import com.dynamicmart.catalog_service.entity.InventoryReservationItem;
import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.entity.VariantStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.CategoryRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.InventoryReservationItemRepository;
import com.dynamicmart.catalog_service.repository.InventoryReservationRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationItemRepository reservationItemRepository;
    private final InventoryItemRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryOperationIdempotency idempotency;
    private final CatalogOutboxService outboxService;

    public InventoryReservationService(InventoryReservationRepository reservationRepository,
                                       InventoryReservationItemRepository reservationItemRepository,
                                       InventoryItemRepository inventoryRepository,
                                       ProductVariantRepository variantRepository,
                                       ProductRepository productRepository,
                                       CategoryRepository categoryRepository,
                                       InventoryOperationIdempotency idempotency,
                                       CatalogOutboxService outboxService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.idempotency = idempotency;
        this.outboxService = outboxService;
    }

    @Transactional
    public InventoryReservationResponse reserve(UUID operationKey, ReserveInventoryRequest request) {
        List<NormalizedItem> items = normalizeItems(request.items());
        NormalizedReserveRequest normalized = new NormalizedReserveRequest(
                request.checkoutSessionId(), request.expiresAt(), items);
        String requestHash = idempotency.hash(normalized);
        idempotency.lock(operationKey);
        var replay = idempotency.replay(operationKey, InventoryOperationType.RESERVE,
                requestHash, InventoryReservationResponse.class);
        if (replay.isPresent()) return replay.get();

        if (!request.expiresAt().isAfter(Instant.now())) {
            throw badRequest("RESERVATION_EXPIRY_INVALID", "Thời hạn giữ tồn phải ở tương lai.");
        }
        reservationRepository.findByCheckoutSessionIdForUpdate(request.checkoutSessionId())
                .ifPresent(existing -> {
                    throw new CatalogException(HttpStatus.CONFLICT, "CHECKOUT_ALREADY_RESERVED",
                            "Checkout Session đã có một lần giữ tồn.");
                });

        List<UUID> variantIds = items.stream().map(NormalizedItem::variantId).toList();
        Map<UUID, ProductVariant> variants = lockPurchasableVariants(variantIds);
        Map<UUID, InventoryItem> inventory = lockInventory(variantIds);
        for (NormalizedItem item : items) {
            InventoryItem stock = inventory.get(item.variantId());
            if (stock.getAvailableQuantity() < item.quantity()) {
                throw new CatalogException(HttpStatus.CONFLICT, "INSUFFICIENT_INVENTORY",
                        "Variant " + variants.get(item.variantId()).getSku()
                                + " không đủ số lượng có thể bán.");
            }
        }

        InventoryReservation reservation = new InventoryReservation(UUID.randomUUID(),
                request.checkoutSessionId(), request.expiresAt());
        reservationRepository.save(reservation);
        List<InventoryReservationItem> savedItems = new ArrayList<>();
        for (NormalizedItem item : items) {
            inventory.get(item.variantId()).reserve(item.quantity());
            savedItems.add(new InventoryReservationItem(UUID.randomUUID(), reservation.getId(),
                    item.variantId(), item.quantity()));
        }
        reservationItemRepository.saveAll(savedItems);
        InventoryReservationResponse response = response(reservation, savedItems, inventory);
        idempotency.save(operationKey, InventoryOperationType.RESERVE, reservation.getId(),
                requestHash, response);
        outboxService.record("InventoryReservation", reservation.getId(), "InventoryReserved",
                response, operationKey);
        return response;
    }

    @Transactional
    public InventoryReservationResponse commit(UUID reservationId, UUID operationKey,
                                               CommitInventoryRequest request) {
        CommitCommand normalized = new CommitCommand(reservationId, request.orderId());
        String requestHash = idempotency.hash(normalized);
        idempotency.lock(operationKey);
        var replay = idempotency.replay(operationKey, InventoryOperationType.COMMIT,
                requestHash, InventoryReservationResponse.class);
        if (replay.isPresent()) return replay.get();

        InventoryReservation reservation = requireReservationForUpdate(reservationId);
        if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) {
            if (!request.orderId().equals(reservation.getOrderId())) {
                throw new CatalogException(HttpStatus.CONFLICT, "RESERVATION_ORDER_MISMATCH",
                        "Reservation đã được chốt cho Order khác.");
            }
            InventoryReservationResponse current = currentResponse(reservation);
            idempotency.save(operationKey, InventoryOperationType.COMMIT, reservationId,
                    requestHash, current);
            return current;
        }
        requireReserved(reservation, "chốt");
        if (reservation.isExpiredAt(Instant.now())) {
            throw new CatalogException(HttpStatus.CONFLICT, "RESERVATION_EXPIRED",
                    "Reservation đã hết hạn và không thể chốt.");
        }

        List<InventoryReservationItem> items = reservationItems(reservationId);
        Map<UUID, InventoryItem> inventory = lockInventory(
                items.stream().map(InventoryReservationItem::getVariantId).toList());
        for (InventoryReservationItem item : items) {
            inventory.get(item.getVariantId()).commit(item.getQuantity());
        }
        reservation.commit(request.orderId(), Instant.now());
        InventoryReservationResponse response = response(reservation, items, inventory);
        idempotency.save(operationKey, InventoryOperationType.COMMIT, reservationId,
                requestHash, response);
        outboxService.record("InventoryReservation", reservationId, "InventoryCommitted",
                response, operationKey);
        return response;
    }

    @Transactional
    public InventoryReservationResponse release(UUID reservationId, UUID operationKey,
                                                ReleaseInventoryRequest request) {
        String reason = request.reason().trim().toUpperCase(Locale.ROOT);
        ReleaseCommand normalized = new ReleaseCommand(reservationId, reason, false);
        return releaseInternal(operationKey, normalized);
    }

    @Transactional
    public void expire(UUID reservationId) {
        UUID operationKey = UUID.nameUUIDFromBytes(
                ("inventory-expire:" + reservationId).getBytes(StandardCharsets.UTF_8));
        releaseInternal(operationKey, new ReleaseCommand(reservationId,
                "RESERVATION_EXPIRED", true));
    }

    @Transactional(readOnly = true)
    public InventoryReservationResponse get(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(this::reservationNotFound);
        return currentResponse(reservation);
    }

    private InventoryReservationResponse releaseInternal(UUID operationKey, ReleaseCommand command) {
        String requestHash = idempotency.hash(command);
        idempotency.lock(operationKey);
        var replay = idempotency.replay(operationKey, InventoryOperationType.RELEASE,
                requestHash, InventoryReservationResponse.class);
        if (replay.isPresent()) return replay.get();

        InventoryReservation reservation = requireReservationForUpdate(command.reservationId());
        if (reservation.getStatus() == InventoryReservationStatus.COMMITTED) {
            throw new CatalogException(HttpStatus.CONFLICT, "RESERVATION_ALREADY_COMMITTED",
                    "Tồn kho đã được chốt và không thể trả bằng thao tác này.");
        }
        if (reservation.getStatus() == InventoryReservationStatus.RELEASED
                || reservation.getStatus() == InventoryReservationStatus.EXPIRED) {
            InventoryReservationResponse current = currentResponse(reservation);
            idempotency.save(operationKey, InventoryOperationType.RELEASE, reservation.getId(),
                    requestHash, current);
            return current;
        }
        if (command.expiration() && !reservation.isExpiredAt(Instant.now())) return currentResponse(reservation);

        List<InventoryReservationItem> items = reservationItems(reservation.getId());
        Map<UUID, InventoryItem> inventory = lockInventory(
                items.stream().map(InventoryReservationItem::getVariantId).toList());
        for (InventoryReservationItem item : items) {
            inventory.get(item.getVariantId()).release(item.getQuantity());
        }
        if (command.expiration()) reservation.expire(Instant.now());
        else reservation.release(command.reason(), Instant.now());
        InventoryReservationResponse response = response(reservation, items, inventory);
        idempotency.save(operationKey, InventoryOperationType.RELEASE, reservation.getId(),
                requestHash, response);
        outboxService.record("InventoryReservation", reservation.getId(),
                command.expiration() ? "InventoryReservationExpired" : "InventoryReleased",
                response, operationKey);
        return response;
    }

    private Map<UUID, ProductVariant> lockPurchasableVariants(List<UUID> variantIds) {
        List<ProductVariant> initial = variantRepository.findAllByIdIn(variantIds);
        if (initial.size() != variantIds.size()) throw variantUnavailable();
        List<UUID> productIds = initial.stream().map(ProductVariant::getProductId).distinct().sorted().toList();
        Map<UUID, Product> products = indexProducts(productRepository.findAllByIdInForShare(productIds));
        Map<UUID, ProductVariant> variants = indexVariants(
                variantRepository.findAllByIdInForShare(variantIds));
        if (products.size() != productIds.size() || variants.size() != variantIds.size()) {
            throw variantUnavailable();
        }
        Instant now = Instant.now();
        Set<UUID> categoryIds = new HashSet<>();
        for (ProductVariant variant : variants.values()) {
            Product product = products.get(variant.getProductId());
            if (variant.getStatus() != VariantStatus.ACTIVE || product == null || !product.isPublicAt(now)) {
                throw variantUnavailable();
            }
            categoryIds.add(product.getCategoryId());
        }
        long activeCategories = categoryRepository.findAllById(categoryIds).stream()
                .filter(category -> category.getStatus() == CatalogStatus.ACTIVE)
                .count();
        if (activeCategories != categoryIds.size()) throw variantUnavailable();
        return variants;
    }

    private Map<UUID, InventoryItem> lockInventory(List<UUID> variantIds) {
        List<UUID> sorted = variantIds.stream().distinct().sorted().toList();
        List<InventoryItem> locked = inventoryRepository.findAllByVariantIdInForUpdate(sorted);
        if (locked.size() != sorted.size()) {
            throw new CatalogException(HttpStatus.CONFLICT, "INVENTORY_NOT_INITIALIZED",
                    "Một hoặc nhiều Variant chưa có bản ghi tồn kho.");
        }
        Map<UUID, InventoryItem> result = new HashMap<>();
        locked.forEach(item -> result.put(item.getVariantId(), item));
        return result;
    }

    private List<NormalizedItem> normalizeItems(List<InventoryReservationItemRequest> requested) {
        Set<UUID> unique = new HashSet<>();
        List<NormalizedItem> result = new ArrayList<>();
        for (InventoryReservationItemRequest item : requested) {
            if (item.variantId() == null || item.quantity() <= 0) {
                throw badRequest("RESERVATION_ITEM_INVALID", "Variant và số lượng giữ tồn không hợp lệ.");
            }
            if (!unique.add(item.variantId())) {
                throw badRequest("RESERVATION_ITEM_DUPLICATED",
                        "Một Variant chỉ được xuất hiện một lần trong yêu cầu giữ tồn.");
            }
            result.add(new NormalizedItem(item.variantId(), item.quantity()));
        }
        result.sort(java.util.Comparator.comparing(NormalizedItem::variantId));
        return List.copyOf(result);
    }

    private InventoryReservationResponse currentResponse(InventoryReservation reservation) {
        List<InventoryReservationItem> items = reservationItems(reservation.getId());
        List<UUID> variantIds = items.stream().map(InventoryReservationItem::getVariantId).toList();
        Map<UUID, InventoryItem> inventory = new HashMap<>();
        inventoryRepository.findAllByVariantIdIn(variantIds)
                .forEach(item -> inventory.put(item.getVariantId(), item));
        return response(reservation, items, inventory);
    }

    private InventoryReservationResponse response(InventoryReservation reservation,
                                                  List<InventoryReservationItem> items,
                                                  Map<UUID, InventoryItem> inventory) {
        List<InventoryReservationItemResponse> itemResponses = items.stream()
                .map(item -> new InventoryReservationItemResponse(item.getVariantId(), item.getQuantity(),
                        inventory.containsKey(item.getVariantId())
                                ? inventory.get(item.getVariantId()).getAvailableQuantity() : 0))
                .toList();
        return new InventoryReservationResponse(reservation.getId(), reservation.getCheckoutSessionId(),
                reservation.getOrderId(), reservation.getStatus(), reservation.getExpiresAt(),
                reservation.getCommittedAt(), reservation.getReleasedAt(), reservation.getReleaseReason(),
                itemResponses);
    }

    private InventoryReservation requireReservationForUpdate(UUID reservationId) {
        return reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(this::reservationNotFound);
    }

    private List<InventoryReservationItem> reservationItems(UUID reservationId) {
        List<InventoryReservationItem> items = reservationItemRepository
                .findAllByReservationIdOrderByVariantIdAsc(reservationId);
        if (items.isEmpty()) {
            throw new CatalogException(HttpStatus.CONFLICT, "RESERVATION_ITEMS_MISSING",
                    "Reservation không có dữ liệu dòng hàng.");
        }
        return items;
    }

    private void requireReserved(InventoryReservation reservation, String action) {
        if (reservation.getStatus() != InventoryReservationStatus.RESERVED) {
            throw new CatalogException(HttpStatus.CONFLICT, "RESERVATION_STATE_INVALID",
                    "Trạng thái Reservation không cho phép " + action + " tồn kho.");
        }
    }

    private Map<UUID, Product> indexProducts(List<Product> products) {
        Map<UUID, Product> result = new HashMap<>();
        products.forEach(product -> result.put(product.getId(), product));
        return result;
    }

    private Map<UUID, ProductVariant> indexVariants(List<ProductVariant> variants) {
        Map<UUID, ProductVariant> result = new HashMap<>();
        variants.forEach(variant -> result.put(variant.getId(), variant));
        return result;
    }

    private CatalogException variantUnavailable() {
        return new CatalogException(HttpStatus.CONFLICT, "VARIANT_NOT_PURCHASABLE",
                "Một hoặc nhiều Variant không còn ở trạng thái có thể mua.");
    }

    private CatalogException reservationNotFound() {
        return new CatalogException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                "Không tìm thấy Inventory Reservation.");
    }

    private CatalogException badRequest(String code, String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, code, message);
    }

    private record NormalizedItem(UUID variantId, int quantity) {
    }

    private record NormalizedReserveRequest(UUID checkoutSessionId, Instant expiresAt,
                                            List<NormalizedItem> items) {
    }

    private record CommitCommand(UUID reservationId, UUID orderId) {
    }

    private record ReleaseCommand(UUID reservationId, String reason, boolean expiration) {
    }
}
