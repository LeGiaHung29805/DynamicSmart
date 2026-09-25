export type Profile = { id: string; email: string; fullName: string; phone: string; role: "CUSTOMER" | "ADMIN"; status: string; lastLoginAt?: string; createdAt?: string; updatedAt?: string };
export type Address = { id: string; recipientName: string; phone: string; addressLine: string; provinceId: number; provinceName: string; wardId: number; wardName: string; defaultAddress: boolean; status: string };
export type VoucherWalletItem = { id: string; code: string; name: string; description: string; distributionMode: "DEFAULT_FOR_ELIGIBLE" | "ASSIGNED_ONLY" | "CODE_ONLY"; eligible: boolean; ineligibleReason?: string; endsAt: string; scope: string };
export type LocationOption = { id: number; name: string };
export type AdminUser = { id: string; email: string; fullName: string; phone?: string; role: "CUSTOMER" | "ADMIN"; status: "ACTIVE" | "LOCKED" | "DISABLED"; lastLoginAt?: string; createdAt: string };
export type PageResult<T> = { content: T[]; totalElements: number; totalPages: number; number: number; size: number };
