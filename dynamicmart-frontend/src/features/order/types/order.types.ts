export type OrderStatus =
  | "PENDING_PAYMENT"
  | "CONFIRMED"
  | "PACKING"
  | "SHIPPING"
  | "HANDOVER_PENDING"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED";

export type PaymentTiming = "PREPAID" | "POSTPAID" | "NOT_REQUIRED";
export type PaymentMethod = "VNPAY" | "ZALOPAY" | "PAYOS" | "BANK_QR" | "COD" | "FREE";
export type OrderAction = "PACK" | "SHIP" | "HANDOVER" | "CONFIRM_RECEIVED";

export type OrderMoney = {
  itemsListSubtotalVnd: number;
  directSaleDiscountVnd: number;
  itemsSubtotalVnd: number;
  productDiscountVnd: number;
  orderDiscountVnd: number;
  shippingFeeVnd: number;
  shippingDiscountVnd: number;
  finalTotalVnd: number;
  currency: string;
};

export type OrderItem = {
  itemId: string;
  productId: string;
  variantId: string;
  sku: string;
  productName: string;
  variantName?: string;
  imageUrl?: string;
  listPriceVnd: number;
  directSalePromotionId?: string;
  directSaleDiscountVnd: number;
  unitPriceVnd: number;
  quantity: number;
  productDiscountVnd: number;
  orderDiscountVnd: number;
  lineTotalVnd: number;
  weightGrams: number;
};

export type OrderAddress = {
  recipientName: string;
  phone: string;
  addressLine: string;
  provinceId: number;
  wardId: number;
  provinceName: string;
  wardName: string;
};

export type OrderVoucher = {
  voucherId: string;
  voucherCode: string;
  scope: string;
  discountMethod: string;
  discountValue?: number;
  eligibleSubtotalVnd: number;
  discountAmountVnd: number;
  shippingDiscountVnd: number;
};

export type OrderShipping = {
  quoteId: string;
  provider: string;
  serviceId: number;
  serviceName: string;
  feeVnd: number;
  shippingDiscountVnd: number;
  payableFeeVnd: number;
  eta?: string;
  etaText?: string;
  totalWeightGrams?: number;
  packageLengthCm?: number;
  packageWidthCm?: number;
  packageHeightCm?: number;
  toProvinceId?: number;
  toWardId?: number;
  toProvinceName?: string;
  toWardName?: string;
  quotedAt?: string;
};

export type OrderTimelineEntry = {
  historyId: string;
  fromStatus?: OrderStatus;
  toStatus: OrderStatus;
  actorType: string;
  actorId?: string;
  reason?: string;
  correlationId?: string;
  createdAt: string;
};

export type OrderSummary = {
  orderId: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  paymentTiming: PaymentTiming;
  paymentMethod: PaymentMethod;
  finalTotalVnd: number;
  currency: string;
  paymentDueAt?: string;
  createdAt: string;
  updatedAt: string;
};

export type OrderPage = {
  content: OrderSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  sort: string;
};

export type OrderDetail = {
  orderId: string;
  orderNumber: string;
  customerId: string;
  status: OrderStatus;
  paymentTiming: PaymentTiming;
  paymentMethod: PaymentMethod;
  money: OrderMoney;
  paymentDueAt?: string;
  paymentSucceededAt?: string;
  shipmentDeliveredAt?: string;
  cancelReason?: string;
  cancelledAt?: string;
  confirmedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  availableActions: OrderAction[];
  items: OrderItem[];
  address: OrderAddress;
  vouchers: OrderVoucher[];
  shipping: OrderShipping;
  timeline: OrderTimelineEntry[];
};

export type OrderTimeline = { orderId: string; timeline: OrderTimelineEntry[] };

export type OrderCommandResult = {
  orderId: string;
  status: OrderStatus;
  appliedTransitions: OrderStatus[];
  updatedAt: string;
  replay: boolean;
};

export type OrderQuery = {
  page?: number;
  size?: number;
  status?: OrderStatus | "";
  orderNumber?: string;
  customerId?: string;
  createdFrom?: string;
  createdTo?: string;
  sort?: string;
};
