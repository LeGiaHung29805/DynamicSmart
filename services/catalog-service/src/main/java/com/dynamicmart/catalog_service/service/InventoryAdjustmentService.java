package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.request.AdjustInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.AdminInventoryItemResponse;
import com.dynamicmart.catalog_service.dto.response.InventoryAdjustmentResponse;
import com.dynamicmart.catalog_service.dto.response.PageResponse;
import com.dynamicmart.catalog_service.entity.InventoryAdjustment;
import com.dynamicmart.catalog_service.entity.InventoryItem;
import com.dynamicmart.catalog_service.entity.Product;
import com.dynamicmart.catalog_service.entity.ProductVariant;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.InventoryAdjustmentRepository;
import com.dynamicmart.catalog_service.repository.InventoryItemRepository;
import com.dynamicmart.catalog_service.repository.ProductRepository;
import com.dynamicmart.catalog_service.repository.ProductVariantRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustmentService {
    private final InventoryItemRepository inventoryRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final PostgresAdvisoryLock advisoryLock;
    private final CatalogOutboxService outboxService;

    public InventoryAdjustmentService(InventoryItemRepository inventoryRepository,
                                      InventoryAdjustmentRepository adjustmentRepository,
                                      ProductVariantRepository variantRepository,
                                      ProductRepository productRepository,
                                      PostgresAdvisoryLock advisoryLock,
                                      CatalogOutboxService outboxService) {
        this.inventoryRepository = inventoryRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.advisoryLock = advisoryLock;
        this.outboxService = outboxService;
    }

    @Transactional
    public InventoryAdjustmentResponse adjust(UUID variantId, UUID operationKey, UUID actorAdminId,
                                              AdjustInventoryRequest request) {
        if (request.quantityDelta() == null || request.quantityDelta() == 0) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "INVENTORY_DELTA_INVALID",
                    "Lượng điều chỉnh tồn kho phải khác 0.");
        }
        String reason = request.reason().trim();
        advisoryLock.lock(operationKey);
        var previous = adjustmentRepository.findByOperationKey(operationKey);
        if (previous.isPresent()) {
            InventoryAdjustment adjustment = previous.get();
            if (!adjustment.getVariantId().equals(variantId)
                    || adjustment.getQuantityDelta() != request.quantityDelta()
                    || !adjustment.getReason().equals(reason)
                    || !adjustment.getActorAdminId().equals(actorAdminId)) {
                throw new CatalogException(HttpStatus.CONFLICT, "IDEMPOTENCY_PAYLOAD_MISMATCH",
                        "Idempotency-Key đã được dùng với nội dung điều chỉnh khác.");
            }
            return adjustmentResponse(adjustment);
        }
        if (!variantRepository.existsById(variantId)) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                    "Không tìm thấy Variant cần điều chỉnh tồn kho.");
        }
        InventoryItem inventory = inventoryRepository.findByVariantIdForUpdate(variantId)
                .orElseThrow(() -> inventoryNotFound(variantId));
        int before = inventory.getOnHandQuantity();
        try {
            inventory.adjust(request.quantityDelta());
        } catch (ArithmeticException | IllegalStateException exception) {
            throw new CatalogException(HttpStatus.CONFLICT, "INVENTORY_ADJUSTMENT_REJECTED",
                    "Tồn sau điều chỉnh không được âm hoặc thấp hơn lượng đang giữ.");
        }
        InventoryAdjustment adjustment = adjustmentRepository.saveAndFlush(new InventoryAdjustment(
                UUID.randomUUID(), operationKey, variantId, request.quantityDelta(), before,
                inventory.getOnHandQuantity(), reason, actorAdminId));
        InventoryAdjustmentResponse response = adjustmentResponse(adjustment);
        outboxService.record("InventoryItem", variantId, "InventoryAdjusted", response, operationKey);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryAdjustmentResponse> history(UUID variantId, int page, int size) {
        requireVariant(variantId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<InventoryAdjustment> adjustments = adjustmentRepository
                .findAllByVariantIdOrderByCreatedAtDesc(variantId, PageRequest.of(safePage, safeSize));
        List<InventoryAdjustmentResponse> content = adjustments.getContent().stream()
                .map(this::adjustmentResponse)
                .toList();
        return new PageResponse<>(content, adjustments.getNumber(), adjustments.getSize(),
                adjustments.getTotalElements(), adjustments.getTotalPages(), adjustments.isFirst(),
                adjustments.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminInventoryItemResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<InventoryItem> inventoryPage = inventoryRepository.findAll(PageRequest.of(safePage,
                safeSize, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<UUID> variantIds = inventoryPage.getContent().stream()
                .map(InventoryItem::getVariantId).toList();
        Map<UUID, ProductVariant> variants = new HashMap<>();
        variantRepository.findAllByIdIn(variantIds)
                .forEach(variant -> variants.put(variant.getId(), variant));
        Map<UUID, Product> products = new HashMap<>();
        productRepository.findAllById(variants.values().stream()
                        .map(ProductVariant::getProductId).distinct().toList())
                .forEach(product -> products.put(product.getId(), product));
        List<AdminInventoryItemResponse> content = inventoryPage.getContent().stream()
                .map(inventory -> inventoryResponse(inventory, variants, products))
                .toList();
        return new PageResponse<>(content, inventoryPage.getNumber(), inventoryPage.getSize(),
                inventoryPage.getTotalElements(), inventoryPage.getTotalPages(), inventoryPage.isFirst(),
                inventoryPage.isLast());
    }

    private AdminInventoryItemResponse inventoryResponse(InventoryItem inventory,
                                                          Map<UUID, ProductVariant> variants,
                                                          Map<UUID, Product> products) {
        ProductVariant variant = variants.get(inventory.getVariantId());
        if (variant == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "INVENTORY_VARIANT_INVALID",
                    "Tồn kho đang tham chiếu Variant không tồn tại.");
        }
        Product product = products.get(variant.getProductId());
        if (product == null) {
            throw new CatalogException(HttpStatus.CONFLICT, "VARIANT_PRODUCT_INVALID",
                    "Variant đang tham chiếu Product không tồn tại.");
        }
        return new AdminInventoryItemResponse(variant.getId(), product.getId(), product.getName(),
                variant.getName(), variant.getSku(), variant.getStatus(), inventory.getOnHandQuantity(),
                inventory.getReservedQuantity(), inventory.getAvailableQuantity(), inventory.getVersion());
    }

    private InventoryAdjustmentResponse adjustmentResponse(InventoryAdjustment adjustment) {
        return new InventoryAdjustmentResponse(adjustment.getId(), adjustment.getOperationKey(),
                adjustment.getVariantId(), adjustment.getQuantityDelta(), adjustment.getOnHandBefore(),
                adjustment.getOnHandAfter(), adjustment.getReason(),
                adjustment.getActorAdminId(), adjustment.getCreatedAt());
    }

    private void requireVariant(UUID variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                    "Không tìm thấy Variant.");
        }
    }

    private CatalogException inventoryNotFound(UUID variantId) {
        return new CatalogException(HttpStatus.CONFLICT, "INVENTORY_NOT_INITIALIZED",
                "Variant " + variantId + " chưa có bản ghi tồn kho.");
    }
}
