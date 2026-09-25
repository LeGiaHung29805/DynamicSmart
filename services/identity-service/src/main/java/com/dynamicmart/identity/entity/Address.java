package com.dynamicmart.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses")
public class Address {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "recipient_name", nullable = false, length = 150) private String recipientName;
    @Column(nullable = false, length = 20) private String phone;
    @Column(name = "address_line", nullable = false, length = 500) private String addressLine;
    @Column(name = "province_id", nullable = false) private int provinceId;
    @Column(name = "ward_id", nullable = false) private int wardId;
    @Column(name = "province_name", nullable = false, length = 150) private String provinceName;
    @Column(name = "ward_name", nullable = false, length = 150) private String wardName;
    @Column(name = "location_validated_at", nullable = false) private Instant locationValidatedAt;
    @Column(name = "is_default", nullable = false) private boolean defaultAddress;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AddressStatus status;
    @Column(name = "deactivated_at") private Instant deactivatedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Address() { }

    public Address(UUID id, UUID userId, String recipientName, String phone, String addressLine,
                   int provinceId, int wardId, String provinceName, String wardName, boolean defaultAddress, Instant now) {
        this.id = id; this.userId = userId; this.status = AddressStatus.ACTIVE; this.createdAt = now;
        update(recipientName, phone, addressLine, provinceId, wardId, provinceName, wardName, defaultAddress, now);
    }

    public void update(String recipientName, String phone, String addressLine, int provinceId, int wardId,
                       String provinceName, String wardName, boolean defaultAddress, Instant now) {
        this.recipientName = recipientName; this.phone = phone; this.addressLine = addressLine;
        this.provinceId = provinceId; this.wardId = wardId; this.provinceName = provinceName; this.wardName = wardName;
        this.defaultAddress = defaultAddress; this.locationValidatedAt = now; this.updatedAt = now;
    }

    public void setDefaultAddress(boolean value, Instant now) { this.defaultAddress = value; this.updatedAt = now; }
    public void deactivate(Instant now) { this.status = AddressStatus.INACTIVE; this.defaultAddress = false; this.deactivatedAt = now; this.updatedAt = now; }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public int getProvinceId() { return provinceId; }
    public int getWardId() { return wardId; }
    public String getProvinceName() { return provinceName; }
    public String getWardName() { return wardName; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public AddressStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
