package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.exception.OrderException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ReservationGatewayFallbackConfiguration {
    @Bean
    @ConditionalOnMissingBean(VoucherReservationGateway.class)
    VoucherReservationGateway unavailableVoucherReservationGateway() {
        return new VoucherReservationGateway() {
            @Override
            public Reservation reserve(ReserveVoucherRequest request) {
                throw unavailable("Cart/Voucher");
            }

            @Override
            public void release(ReleaseVoucherRequest request) {
                throw unavailable("Cart/Voucher");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(InventoryReservationGateway.class)
    InventoryReservationGateway unavailableInventoryReservationGateway() {
        return new InventoryReservationGateway() {
            @Override
            public Reservation reserve(ReserveInventoryRequest request) {
                throw unavailable("Catalog/Inventory");
            }

            @Override
            public void release(ReleaseInventoryRequest request) {
                throw unavailable("Catalog/Inventory");
            }
        };
    }

    private OrderException unavailable(String dependency) {
        return new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "RESERVATION_SOURCE_UNAVAILABLE",
                dependency + " Service chưa sẵn sàng cho reservation.");
    }
}
