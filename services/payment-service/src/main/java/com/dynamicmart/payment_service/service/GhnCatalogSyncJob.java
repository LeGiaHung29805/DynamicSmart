package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.GhnClient;
import com.dynamicmart.payment_service.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GhnCatalogSyncJob {
    private static final Logger log = LoggerFactory.getLogger(GhnCatalogSyncJob.class);

    private final GhnClient ghn;
    private final GhnLocationSyncService locations;

    public GhnCatalogSyncJob(GhnClient ghn, GhnLocationSyncService locations) {
        this.ghn = ghn;
        this.locations = locations;
    }

    @Scheduled(
            initialDelayString = "${app.ghn.catalog-sync-initial-delay-ms:1000}",
            fixedDelayString = "${app.ghn.catalog-sync-delay-ms:86400000}")
    public void syncCatalog() {
        if (!ghn.isCatalogConfigured()) {
            log.info("GHN catalog sync skipped because GHN_BASE_URL or GHN_TOKEN is not configured.");
            return;
        }
        try {
            var result = locations.sync();
            log.info("GHN catalog synchronized: {} provinces, {} districts, {} wards.",
                    result.provinceCount(), result.districtCount(), result.wardCount());
        } catch (PaymentException exception) {
            log.warn("GHN catalog sync failed with {}. Existing catalog remains active.", exception.getCode());
        } catch (RuntimeException exception) {
            log.error("GHN catalog sync failed unexpectedly. Existing catalog remains active.", exception);
        }
    }
}
