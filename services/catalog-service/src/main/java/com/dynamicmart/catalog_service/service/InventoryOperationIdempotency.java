package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.entity.InventoryOperationLog;
import com.dynamicmart.catalog_service.entity.InventoryOperationType;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.repository.InventoryOperationLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class InventoryOperationIdempotency {
    private final InventoryOperationLogRepository operationRepository;
    private final PostgresAdvisoryLock advisoryLock;
    private final ObjectMapper objectMapper;

    public InventoryOperationIdempotency(InventoryOperationLogRepository operationRepository,
                                         PostgresAdvisoryLock advisoryLock,
                                         ObjectMapper objectMapper) {
        this.operationRepository = operationRepository;
        this.advisoryLock = advisoryLock;
        this.objectMapper = objectMapper;
    }

    public void lock(UUID operationKey) {
        advisoryLock.lock(operationKey);
    }

    public <T> Optional<T> replay(UUID operationKey, InventoryOperationType operationType,
                                  String requestHash, Class<T> responseType) {
        return operationRepository.findById(operationKey).map(log -> {
            if (log.getOperationType() != operationType) {
                throw new CatalogException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã được dùng cho thao tác khác.");
            }
            JsonNode envelope = log.getResultPayload();
            JsonNode savedHash = envelope.get("requestHash");
            JsonNode response = envelope.get("response");
            if (savedHash == null || response == null || !requestHash.equals(savedHash.asText())) {
                throw new CatalogException(HttpStatus.CONFLICT, "IDEMPOTENCY_PAYLOAD_MISMATCH",
                        "Idempotency-Key đã được dùng với nội dung yêu cầu khác.");
            }
            try {
                return objectMapper.treeToValue(response, responseType);
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot restore inventory idempotency response", exception);
            }
        });
    }

    public void save(UUID operationKey, InventoryOperationType operationType, UUID reservationId,
                     String requestHash, Object response) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("requestHash", requestHash);
        envelope.set("response", objectMapper.valueToTree(response));
        operationRepository.save(new InventoryOperationLog(operationKey, operationType,
                reservationId, envelope));
    }

    public String hash(Object request) {
        try {
            byte[] serialized = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialized));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize idempotent request", exception);
        }
    }
}
