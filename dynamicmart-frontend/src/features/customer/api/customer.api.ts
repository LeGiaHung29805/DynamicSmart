import { apiClient } from "@/lib/api/client";
import type { Address, AdminUser, LocationOption, PageResult, Profile, VoucherUsageHistory, VoucherWalletItem } from "../types/customer.types";

export const customerApi = {
  profile: () => apiClient.get<Profile>("/api/v1/profile"),
  updateProfile: (body: Pick<Profile, "fullName" | "phone">) => apiClient.put<Profile>("/api/v1/profile", body),
  addresses: () => apiClient.get<Address[]>("/api/v1/addresses"),
  createAddress: (body: Omit<Address, "id" | "status">) => apiClient.post<Address>("/api/v1/addresses", body),
  updateAddress: (id: string, body: Omit<Address, "id" | "status">) => apiClient.put<Address>(`/api/v1/addresses/${id}`, body),
  makeDefault: (id: string) => apiClient.post<Address>(`/api/v1/addresses/${id}/default`),
  deactivateAddress: (id: string) => apiClient.delete<void>(`/api/v1/addresses/${id}`),
  provinces: () => apiClient.get<LocationOption[]>("/api/v1/locations/provinces"),
  wards: (provinceId: number) => apiClient.get<LocationOption[]>(`/api/v1/locations/wards?provinceId=${provinceId}`),
  vouchers: () => apiClient.get<VoucherWalletItem[]>("/api/v1/cart/vouchers/wallet"),
  previewVoucher: (body: { voucherId?: string; code?: string; orderSubtotalVnd: number; eligibleSubtotalVnd: number; shippingFeeVnd: number; productIds: string[]; categoryIds: string[] }) => apiClient.post<VoucherWalletItem>("/api/v1/cart/vouchers/preview", body),
  voucherHistory: () => apiClient.get<VoucherUsageHistory[]>("/api/v1/cart/vouchers/history"),
  users: (query = "", status = "", role = "") => apiClient.get<PageResult<AdminUser>>(`/api/v1/admin/users?query=${encodeURIComponent(query)}${status ? `&status=${encodeURIComponent(status)}` : ""}${role ? `&role=${encodeURIComponent(role)}` : ""}&page=0&size=100`),
  user: (id: string) => apiClient.get<AdminUser>(`/api/v1/admin/users/${id}`),
  manageUser: (id: string, body: { role?: string; status?: string; reason: string }) => apiClient.patch<AdminUser>(`/api/v1/admin/users/${id}`, body, { headers: { "Idempotency-Key": crypto.randomUUID() } }),
};
