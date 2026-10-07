package com.dynamicmart.catalog_service.service;

import static org.mockito.Mockito.verifyNoInteractions;

import com.dynamicmart.catalog_service.repository.OutboxEventRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

class CatalogOutboxServiceTest {
    @Test
    void standaloneModeDoesNotLeaveEventsForLaterPublishing() {
        OutboxEventRepository repository = Mockito.mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        CatalogOutboxService service = new CatalogOutboxService(repository, objectMapper, false);

        service.record("InventoryReservation", UUID.randomUUID(), "InventoryReserved",
                new Object(), UUID.randomUUID());

        verifyNoInteractions(repository, objectMapper);
    }
}
