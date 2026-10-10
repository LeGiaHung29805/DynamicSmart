package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.GhnClient;
import com.dynamicmart.payment_service.exception.PaymentException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GhnLocationSyncServiceTest {
    @Test
    void keepsCurrentCatalogWhenGhnIsUnavailable() {
        GhnClient ghn = mock(GhnClient.class);
        GhnLocationCatalogWriter writer = mock(GhnLocationCatalogWriter.class);
        when(ghn.provinces()).thenThrow(new PaymentException(
                HttpStatus.BAD_GATEWAY, "GHN_UNAVAILABLE", "GHN unavailable"));

        PaymentException error = assertThrows(PaymentException.class,
                () -> new GhnLocationSyncService(ghn, writer).sync());

        assertEquals("GHN_UNAVAILABLE", error.getCode());
        verify(writer, never()).replaceActiveCatalog(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }
}
