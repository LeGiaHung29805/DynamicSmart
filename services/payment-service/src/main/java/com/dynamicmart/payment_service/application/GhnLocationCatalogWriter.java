package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.LocationSyncResponse;
import com.dynamicmart.payment_service.entity.GhnLocationDistrict;
import com.dynamicmart.payment_service.entity.GhnLocationProvince;
import com.dynamicmart.payment_service.entity.GhnLocationWard;
import com.dynamicmart.payment_service.repository.GhnLocationDistrictRepository;
import com.dynamicmart.payment_service.repository.GhnLocationProvinceRepository;
import com.dynamicmart.payment_service.repository.GhnLocationWardRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GhnLocationCatalogWriter {
    private final GhnLocationProvinceRepository provinces;
    private final GhnLocationDistrictRepository districts;
    private final GhnLocationWardRepository wards;

    public GhnLocationCatalogWriter(GhnLocationProvinceRepository provinces, GhnLocationDistrictRepository districts, GhnLocationWardRepository wards) {
        this.provinces = provinces; this.districts = districts; this.wards = wards;
    }

    @Transactional
    public LocationSyncResponse replaceActiveCatalog(List<GhnLocationSyncService.ProvinceData> provinceData,
                                                      List<GhnLocationSyncService.DistrictData> districtData,
                                                      List<GhnLocationSyncService.WardData> wardData) {
        Instant now = Instant.now();
        provinces.findAll().forEach(item -> item.setActive(false));
        districts.findAll().forEach(item -> item.setActive(false));
        wards.findAll().forEach(item -> item.setActive(false));
        for (var source : provinceData) {
            GhnLocationProvince item = provinces.findById(source.id()).orElseGet(GhnLocationProvince::new);
            item.setId(source.id()); item.setName(source.name()); item.setNameNormalized(source.normalizedName()); item.setGhnUpdatedAt(now); item.setSyncedAt(now); item.setActive(true); provinces.save(item);
        }
        for (var source : districtData) {
            GhnLocationDistrict item = districts.findById(source.id()).orElseGet(GhnLocationDistrict::new);
            item.setId(source.id()); item.setProvinceId(source.provinceId()); item.setName(source.name()); item.setNameNormalized(source.normalizedName()); item.setGhnUpdatedAt(now); item.setSyncedAt(now); item.setActive(true); districts.save(item);
        }
        for (var source : wardData) {
            GhnLocationWard item = wards.findById(source.id()).orElseGet(GhnLocationWard::new);
            item.setId(source.id()); item.setProvinceId(source.provinceId()); item.setDistrictId(source.districtId()); item.setGhnWardCode(source.wardCode()); item.setName(source.name()); item.setNameNormalized(source.normalizedName()); item.setGhnUpdatedAt(now); item.setSyncedAt(now); item.setActive(true); wards.save(item);
        }
        return new LocationSyncResponse(provinceData.size(), districtData.size(), wardData.size());
    }
}
