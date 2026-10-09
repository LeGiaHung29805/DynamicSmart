import type { OrderStatus, PaymentMethod, PaymentTiming } from "@/features/order/types/order.types";

export type CheckoutSource = "CART" | "BUY_NOW";
export type CheckoutStatus = "ACTIVE" | "COMPLETED" | "CANCELLED" | "EXPIRED";

export type CheckoutSessionItem = {
  id: string;
  productId: string;
  variantId: string;
  sku: string;
  productName: string;
  variantName?: string;
  imageUrl?: string;
  listPriceVnd: number;
  directSaleDiscountVnd: number;
  unitPriceVnd: number;
  quantity: number;
  weightGrams: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
};

export type CheckoutSession = {
  id: string;
  source: CheckoutSource;
  cartId?: string;
  addressId?: string;
  status: CheckoutStatus;
  paymentTiming?: PaymentTiming;
  paymentMethod?: PaymentMethod;
  expiresAt: string;
  createdAt: string;
  updatedAt: string;
  items: CheckoutSessionItem[];
};

export type CreateCheckoutSessionInput = {
  source: CheckoutSource;
  cartId?: string;
  variantId?: string;
  quantity?: number;
};

export type UpdateCheckoutSessionInput = {
  addressId?: string;
  paymentTiming?: PaymentTiming;
  paymentMethod?: PaymentMethod;
};

export type CheckoutPreviewVoucher = {
  voucherId: string;
  voucherCode: string;
  scope: string;
  discountAmountVnd: number;
  shippingDiscountVnd: number;
};

export type CheckoutPreview = {
  checkoutSessionId: string;
  addressId: string;
  vouchers: CheckoutPreviewVoucher[];
  shipping: {
    quoteId: string;
    feeVnd: number;
    shippingDiscountVnd: number;
    payableFeeVnd: number;
    serviceId: number;
    serviceName: string;
    eta: string;
    expiresAt: string;
  };
  money: {
    itemsListSubtotalVnd: number;
    directSaleDiscountVnd: number;
    itemsSubtotalVnd: number;
    productDiscountVnd: number;
    orderDiscountVnd: number;
    shippingFeeVnd: number;
    shippingDiscountVnd: number;
    finalTotalVnd: number;
  };
  paymentTiming: PaymentTiming;
  paymentMethod: PaymentMethod;
};

export type CreateOrderResult = {
  orderId: string;
  orderNumber: string;
  status: OrderStatus;
  sagaId: string;
  paymentId?: string;
  paymentDueAt?: string;
  replay: boolean;
  redirectUrl?: string;
};
