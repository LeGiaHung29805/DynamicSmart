package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.entity.GhnLocationProvince;
import com.dynamicmart.payment_service.repository.GhnLocationDistrictRepository;
import com.dynamicmart.payment_service.repository.GhnLocationProvinceRepository;
import com.dynamicmart.payment_service.repository.GhnLocationWardRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationServiceTest {
    @Test
    void loadsCatalogFromGhnWhenDatabaseIsEmpty() {
        GhnLocationProvinceRepository provinces = mock(GhnLocationProvinceRepository.class);
        GhnLocationDistrictRepository districts = mock(GhnLocationDistrictRepository.class);
        GhnLocationWardRepository wards = mock(GhnLocationWardRepository.class);
        GhnLocationSyncService sync = mock(GhnLocationSyncService.class);
        GhnLocationProvince province = new GhnLocationProvince();
        province.setId(201);
        province.setName("Hà Nội");
        when(provinces.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of())
                .thenReturn(List.of(province));

        var result = new LocationService(provinces, districts, wards, sync).provinces();

        verify(sync).sync();
        assertEquals(1, result.size());
        assertEquals("Hà Nội", result.get(0).name());
    }
}
