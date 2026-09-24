package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_addresses")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "source_address_id") private UUID sourceAddressId;
    @Column(name = "recipient_name", nullable = false, length = 150) private String recipientName;
    @Column(nullable = false, length = 20) private String phone;
    @Column(name = "address_line", nullable = false, length = 500) private String addressLine;
    @Column(name = "province_id", nullable = false) private int provinceId;
    @Column(name = "ward_id", nullable = false) private int wardId;
    @Column(name = "province_name", nullable = false, length = 150) private String provinceName;
    @Column(name = "ward_name", nullable = false, length = 150) private String wardName;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static OrderAddress create(
            UUID id,
            UUID orderId,
            UUID sourceAddressId,
            String recipientName,
            String phone,
            String addressLine,
            int provinceId,
            int wardId,
            String provinceName,
            String wardName,
            Instant now) {
        OrderAddress address = new OrderAddress();
        address.id = id;
        address.orderId = orderId;
        address.sourceAddressId = sourceAddressId;
        address.recipientName = recipientName;
        address.phone = phone;
        address.addressLine = addressLine;
        address.provinceId = provinceId;
        address.wardId = wardId;
        address.provinceName = provinceName;
        address.wardName = wardName;
        address.createdAt = now;
        return address;
    }
}
