export type CatalogStatus = "ACTIVE" | "INACTIVE";
export type ProductStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "ARCHIVED";
export type VariantStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";
export type AttributeDataType = "TEXT" | "NUMBER" | "DECIMAL" | "BOOLEAN" | "SELECT" | "MULTI_SELECT";
export type AttributeAppliesTo = "PRODUCT" | "VARIANT";

export interface CatalogFilters {
  q?: string;
  category?: string;
  minimumPriceVnd?: string;
  maximumPriceVnd?: string;
  attribute?: string[];
  sort?: string;
  page?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CategoryBrief {
  id: string;
  code: string;
  name: string;
  slug: string;
}

export interface CategoryTree extends CategoryBrief {
  description?: string | null;
  sortOrder: number;
  children: CategoryTree[];
}

export interface CatalogFilterOption {
  code: string;
  label: string;
}

export interface CatalogFilterDefinition {
  attributeId: string;
  code: string;
  name: string;
  dataType: AttributeDataType;
  appliesTo: AttributeAppliesTo;
  options: CatalogFilterOption[];
}

export interface ProductImage {
  id: string;
  productId: string;
  variantId?: string | null;
  imageUrl: string;
  altText?: string | null;
  sortOrder: number;
  primary: boolean;
}

export interface VariantPrice {
  listPriceVnd: number;
  salePriceVnd?: number | null;
  directSaleDiscountVnd: number;
  directSalePercent?: number | null;
  directSaleEndsAt?: string | null;
  directSalePromotionId?: string | null;
}

export interface AttributeValue {
  id: string;
  attributeId: string;
  attributeCode: string;
  attributeName: string;
  dataType: AttributeDataType;
  value: unknown;
}

export interface InventoryAvailability {
  onHandQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
}

export interface ProductVariant {
  id: string;
  productId: string;
  sku: string;
  name?: string | null;
  price: VariantPrice;
  weightGrams: number;
  lengthCm?: number | null;
  widthCm?: number | null;
  heightCm?: number | null;
  status: VariantStatus;
  sortOrder: number;
  inventory: InventoryAvailability;
  purchasable: boolean;
  attributes: AttributeValue[];
  images: ProductImage[];
}

export interface ProductSummary {
  id: string;
  name: string;
  slug: string;
  shortDescription?: string | null;
  category: CategoryBrief;
  primaryImage?: ProductImage | null;
  representativeVariantId: string;
  representativePrice: VariantPrice;
  minimumListPriceVnd: number;
  maximumListPriceVnd: number;
  inStock: boolean;
  featured: boolean;
  bestSeller: boolean;
  voucherEligible: boolean;
  publishedAt: string;
}

export interface ProductDetail {
  id: string;
  name: string;
  slug: string;
  shortDescription?: string | null;
  description?: string | null;
  category: CategoryBrief;
  featured: boolean;
  publishedAt: string;
  attributes: AttributeValue[];
  images: ProductImage[];
  variants: ProductVariant[];
}

export interface CategoryAdmin extends CategoryBrief {
  parentId?: string | null;
  description?: string | null;
  status: CatalogStatus;
  sortOrder: number;
}

export interface AttributeOptionAdmin {
  id: string;
  attributeId: string;
  code: string;
  label: string;
  sortOrder: number;
  status: CatalogStatus;
}

export interface AttributeAdmin {
  id: string;
  code: string;
  name: string;
  dataType: AttributeDataType;
  validationConfig?: unknown;
  status: CatalogStatus;
  options: AttributeOptionAdmin[];
}

export interface CategoryAttributeAdmin {
  id: string;
  categoryId: string;
  attributeId: string;
  attributeCode: string;
  attributeName: string;
  appliesTo: AttributeAppliesTo;
  required: boolean;
  filterable: boolean;
  sortOrder: number;
}

export interface AdminProduct {
  id: string;
  category: CategoryBrief;
  name: string;
  slug: string;
  shortDescription?: string | null;
  description?: string | null;
  status: ProductStatus;
  publishedAt?: string | null;
  featured: boolean;
  defaultWeightGrams?: number | null;
  defaultLengthCm?: number | null;
  defaultWidthCm?: number | null;
  defaultHeightCm?: number | null;
  attributes: AttributeValue[];
  images: ProductImage[];
  variants: ProductVariant[];
}

export interface AdminInventoryItem {
  variantId: string;
  productId: string;
  productName: string;
  variantName?: string | null;
  sku: string;
  variantStatus: VariantStatus;
  onHandQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  version: number;
}

export interface InventoryAdjustment {
  id: string;
  operationKey: string;
  variantId: string;
  quantityDelta: number;
  onHandBefore: number;
  onHandAfter: number;
  reason: string;
  actorAdminId: string;
  createdAt: string;
}
