package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutItem;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutSelection;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreview;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.ItemSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.MoneySnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.VoucherSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderCreationRevalidationServiceTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID CART_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID MERCHANDISE_VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID SHIPPING_VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PROMOTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Test
    void revalidatesEveryMutableSourceAndReturnsAuthoritativeInput() {
        Fixture fixture = readyFixture(context(money(185_000)));

        var result = fixture.service.revalidate(CUSTOMER_ID, SAGA_ID);

        assertEquals(185_000, result.pricing().finalTotalVnd());
        assertEquals(ADDRESS_ID, result.address().addressId());
        assertEquals(2, result.vouchers().size());
        assertEquals(1, result.lineDiscounts().size());
    }

    @Test
    void rejectsSelectionWhenPriceChangedAfterPreview() {
        Fixture fixture = readyFixture(context(money(185_000)));
        TrustedCheckoutItem changed = trustedItem();
        changed = new TrustedCheckoutItem(
                changed.sourceCartItemId(), changed.sourceCartItemVersion(), changed.productId(), changed.variantId(),
                changed.sku(), changed.productName(), changed.variantName(), changed.imageUrl(),
                110_000, changed.directSalePromotionId(), changed.directSaleDiscountVnd(), changed.quantity(),
                changed.weightGrams(), changed.lengthCm(), changed.widthCm(), changed.heightCm());
        when(fixture.selections.loadSelectedCartItems(CUSTOMER_ID, CART_ID))
                .thenReturn(new TrustedCheckoutSelection(CART_ID, List.of(changed)));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.revalidate(CUSTOMER_ID, SAGA_ID));

        assertEquals("CHECKOUT_SELECTION_CHANGED", exception.getCode());
    }

    @Test
    void rejectsAddressWhenLocationNoLongerMatchesQuote() {
        Fixture fixture = readyFixture(context(money(185_000)));
        when(fixture.addresses.loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID)).thenReturn(
                new AddressSnapshot(ADDRESS_ID, "Hiếu", "0900000000", "1 Đường A",
                        1, 999, "Hà Nội", "Phường khác"));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.revalidate(CUSTOMER_ID, SAGA_ID));

        assertEquals("CHECKOUT_ADDRESS_CHANGED", exception.getCode());
    }

    @Test
    void rejectsVoucherWhenBenefitChangedAfterPreview() {
        Fixture fixture = readyFixture(context(money(185_000)));
        when(fixture.vouchers.preview(any())).thenReturn(new VoucherPreview(
                List.of(
                        new AppliedVoucher(MERCHANDISE_VOUCHER_ID, "SAVE15", "ORDER_DISCOUNT",
                                "FIXED_AMOUNT", 14_000L, 180_000, 14_000, 0),
                        new AppliedVoucher(SHIPPING_VOUCHER_ID, "SHIP10", "SHIPPING_DISCOUNT",
                                "FIXED_AMOUNT", 10_000L, 30_000, 0, 10_000)),
                List.of(new LineDiscount(VARIANT_ID, 9_000, 5_000))));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.revalidate(CUSTOMER_ID, SAGA_ID));

        assertEquals("CHECKOUT_VOUCHER_CHANGED", exception.getCode());
    }

    @Test
    void rejectsWhenRecalculatedTotalDiffersFromPreviewSnapshot() {
        Fixture fixture = readyFixture(context(money(184_000)));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.revalidate(CUSTOMER_ID, SAGA_ID));

        assertEquals("CHECKOUT_TOTAL_CHANGED", exception.getCode());
    }

    @Test
    void rejectsQuoteWhenPackageMetricsDoNotMatchItems() {
        CreationContext base = context(money(185_000));
        QuoteSnapshot badQuote = new QuoteSnapshot(
                base.quote().checkoutQuoteId(), base.quote().providerQuoteId(), base.quote().provider(),
                base.quote().inputFingerprint(),
                base.quote().feeVnd(), base.quote().shippingDiscountVnd(), base.quote().payableFeeVnd(),
                base.quote().serviceId(), base.quote().serviceName(), base.quote().eta(), base.quote().etaText(), 999,
                base.quote().packageLengthCm(), base.quote().packageWidthCm(), base.quote().packageHeightCm(),
                base.quote().provinceId(), base.quote().wardId(), base.quote().provinceName(),
                base.quote().wardName(), base.quote().expiresAt());
        CreationContext stale = new CreationContext(
                base.sagaId(), base.correlationId(), base.checkoutSessionId(), base.customerId(), base.source(),
                base.cartId(), base.addressId(), base.paymentTiming(), base.paymentMethod(), base.money(),
                base.items(), base.vouchers(), badQuote);
        Fixture fixture = readyFixture(stale);

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.revalidate(CUSTOMER_ID, SAGA_ID));

        assertEquals("SHIPPING_QUOTE_STALE", exception.getCode());
    }

    private Fixture readyFixture(CreationContext context) {
        OrderCreationContextReader contexts = Mockito.mock(OrderCreationContextReader.class);
        CheckoutSelectionGateway selections = Mockito.mock(CheckoutSelectionGateway.class);
        AddressGateway addresses = Mockito.mock(AddressGateway.class);
        VoucherPricingGateway vouchers = Mockito.mock(VoucherPricingGateway.class);
        when(contexts.load(CUSTOMER_ID, SAGA_ID)).thenReturn(context);
        when(selections.loadSelectedCartItems(CUSTOMER_ID, CART_ID))
                .thenReturn(new TrustedCheckoutSelection(CART_ID, List.of(trustedItem())));
        when(addresses.loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID)).thenReturn(address());
        when(vouchers.preview(any())).thenReturn(voucherPreview());
        return new Fixture(selections, addresses, vouchers,
                new OrderCreationRevalidationService(
                        contexts, selections, addresses, vouchers, new CheckoutPricingService()));
    }

    private CreationContext context(MoneySnapshot money) {
        return new CreationContext(
                SAGA_ID, UUID.randomUUID(), SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, CART_ID, ADDRESS_ID,
                PaymentTiming.PREPAID, PaymentMethod.VNPAY, money, List.of(item()),
                List.of(
                        new VoucherSnapshot(MERCHANDISE_VOUCHER_ID, "SAVE15", "ORDER_DISCOUNT", 15_000, 0),
                        new VoucherSnapshot(SHIPPING_VOUCHER_ID, "SHIP10", "SHIPPING_DISCOUNT", 0, 10_000)),
                new QuoteSnapshot(
                        UUID.randomUUID(), UUID.randomUUID(), "GHN", "a".repeat(64),
                        30_000, 10_000, 20_000, 53321, "GHN Express", null, "1-2 ngày",
                        400, 20, 10, 10, 1, 2, "Hà Nội", "Phường A",
                        Instant.parse("2026-09-24T03:00:00Z")));
    }

    private MoneySnapshot money(long finalTotal) {
        return new MoneySnapshot(200_000, 20_000, 180_000, 10_000, 5_000,
                30_000, 10_000, finalTotal);
    }

    private ItemSnapshot item() {
        return new ItemSnapshot(
                CART_ITEM_ID, 3L, PRODUCT_ID, VARIANT_ID, "SKU-1", "Sản phẩm", "Đỏ", null,
                100_000, PROMOTION_ID, 10_000, 90_000, 2, 200, 20, 10, 5);
    }

    private TrustedCheckoutItem trustedItem() {
        ItemSnapshot item = item();
        return new TrustedCheckoutItem(
                item.sourceCartItemId(), item.sourceCartItemVersion(), item.productId(), item.variantId(), item.sku(),
                item.productName(), item.variantName(), item.imageUrl(), item.listPriceVnd(),
                item.directSalePromotionId(), item.directSaleDiscountVnd(), item.quantity(), item.weightGrams(),
                item.lengthCm(), item.widthCm(), item.heightCm());
    }

    private AddressSnapshot address() {
        return new AddressSnapshot(
                ADDRESS_ID, "Hiếu", "0900000000", "1 Đường A", 1, 2, "Hà Nội", "Phường A");
    }

    private VoucherPreview voucherPreview() {
        return new VoucherPreview(
                List.of(
                        new AppliedVoucher(MERCHANDISE_VOUCHER_ID, "SAVE15", "ORDER_DISCOUNT",
                                "FIXED_AMOUNT", 15_000L, 180_000, 15_000, 0),
                        new AppliedVoucher(SHIPPING_VOUCHER_ID, "SHIP10", "SHIPPING_DISCOUNT",
                                "FIXED_AMOUNT", 10_000L, 30_000, 0, 10_000)),
                List.of(new LineDiscount(VARIANT_ID, 10_000, 5_000)));
    }

    private record Fixture(
            CheckoutSelectionGateway selections,
            AddressGateway addresses,
            VoucherPricingGateway vouchers,
            OrderCreationRevalidationService service) {
    }
}
