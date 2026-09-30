package com.dynamicmart.payment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ghn_location_districts")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PUBLIC)
public class GhnLocationDistrict {
    @Id private int id;
    @Column(name = "province_id", nullable = false) private int provinceId;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "name_normalized", nullable = false, length = 150) private String nameNormalized;
    @Column(name = "ghn_updated_at") private Instant ghnUpdatedAt;
    @Column(name = "synced_at", nullable = false) private Instant syncedAt;
    @Column(name = "is_active", nullable = false) private boolean active;
}
