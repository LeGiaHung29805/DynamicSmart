export type PromotionCampaign = { id: string; name: string; kind: "DIRECT_SALE" | "VOUCHER"; value: string; scope: string; status: "ACTIVE" | "DRAFT" | "PAUSED"; period: string };
