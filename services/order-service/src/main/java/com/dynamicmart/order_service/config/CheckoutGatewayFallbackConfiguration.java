package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class CheckoutGatewayFallbackConfiguration {
    @Bean
    @ConditionalOnMissingBean(AddressGateway.class)
    AddressGateway unavailableAddressGateway() {
        return (customerId, addressId) -> {
            throw unavailable("Identity/Address");
        };
    }

    @Bean
    @ConditionalOnMissingBean(VoucherPricingGateway.class)
    VoucherPricingGateway unavailableVoucherPricingGateway() {
        return request -> {
            throw unavailable("Cart/Voucher");
        };
    }

    @Bean
    @ConditionalOnMissingBean(CheckoutSelectionGateway.class)
    CheckoutSelectionGateway unavailableCheckoutSelectionGateway() {
        return new CheckoutSelectionGateway() {
            @Override
            public TrustedCheckoutSelection loadSelectedCartItems(UUID customerId, UUID cartId) {
                throw CheckoutGatewayFallbackConfiguration.this.unavailable("Cart/Catalog");
            }

            @Override
            public TrustedCheckoutSelection loadBuyNowItem(UUID customerId, UUID variantId, int quantity) {
                throw CheckoutGatewayFallbackConfiguration.this.unavailable("Cart/Catalog");
            }
        };
    }

    private OrderException unavailable(String dependency) {
        return new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CHECKOUT_SOURCE_UNAVAILABLE",
                dependency + " Service chưa sẵn sàng cho Checkout.");
    }
}
