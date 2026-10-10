package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.GhnClient;
import com.dynamicmart.payment_service.dto.response.LocationSyncResponse;
import com.dynamicmart.payment_service.exception.PaymentException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GhnCatalogSyncJobTest {
    @Test
    void skipsSyncWhenGhnCatalogIsNotConfigured() {
        GhnClient ghn = mock(GhnClient.class);
        GhnLocationSyncService locations = mock(GhnLocationSyncService.class);

        new GhnCatalogSyncJob(ghn, locations).syncCatalog();

        verify(locations, never()).sync();
    }

    @Test
    void synchronizesConfiguredCatalog() {
        GhnClient ghn = mock(GhnClient.class);
        GhnLocationSyncService locations = mock(GhnLocationSyncService.class);
        when(ghn.isCatalogConfigured()).thenReturn(true);
        when(locations.sync()).thenReturn(new LocationSyncResponse(63, 700, 10_000));

        new GhnCatalogSyncJob(ghn, locations).syncCatalog();

        verify(locations).sync();
    }

    @Test
    void providerFailureDoesNotStopTheScheduler() {
        GhnClient ghn = mock(GhnClient.class);
        GhnLocationSyncService locations = mock(GhnLocationSyncService.class);
        when(ghn.isCatalogConfigured()).thenReturn(true);
        when(locations.sync()).thenThrow(new PaymentException(
                HttpStatus.BAD_GATEWAY, "GHN_UNAVAILABLE", "GHN unavailable"));

        assertDoesNotThrow(() -> new GhnCatalogSyncJob(ghn, locations).syncCatalog());
    }
}
