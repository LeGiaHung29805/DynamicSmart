package com.dynamicmart.payment_service.api;

import com.dynamicmart.payment_service.application.LocationService;
import com.dynamicmart.payment_service.application.GhnLocationSyncService;
import com.dynamicmart.payment_service.application.InternalApiVerifier;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final LocationService locationService;
    private final GhnLocationSyncService syncService;
    private final InternalApiVerifier internalApi;

    public LocationController(LocationService locationService, GhnLocationSyncService syncService, InternalApiVerifier internalApi) { this.locationService = locationService; this.syncService = syncService; this.internalApi = internalApi; }

    @GetMapping("/provinces")
    public List<LocationResponse> provinces() { return locationService.provinces(); }

    @GetMapping("/wards")
    public List<LocationResponse> wards(@RequestParam int provinceId) { return locationService.wards(provinceId); }

    /** Internal contract used by identity-service before it persists an Address. */
    @GetMapping("/validate")
    public LocationValidationResponse validate(@RequestHeader("X-Internal-Api-Key") String apiKey, @RequestParam int provinceId, @RequestParam int wardId) {
        internalApi.require(apiKey);
        return locationService.validate(provinceId, wardId);
    }

    /** Operations-only endpoint; protect with service/admin authentication at the Gateway. */
    @PostMapping("/sync")
    public LocationSyncResponse sync(@RequestHeader("X-Internal-Api-Key") String apiKey) { internalApi.require(apiKey); return syncService.sync(); }
}
