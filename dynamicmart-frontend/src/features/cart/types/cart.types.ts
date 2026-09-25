export type CartLine = { id: string; productName: string; variantName: string; image: string; quantity: number; version: number; selected: boolean; available: boolean; listPrice: number; salePrice: number };
export type CartItemDto = { id: string; productId: string; variantId: string; quantity: number; version: number; selected: boolean; updatedAt: string };
export type CartDto = { id: string; customerId: string; items: CartItemDto[]; updatedAt: string };
