import { apiClient } from "@/lib/api/client";
import type { AdminUser, PageResult, Profile, VoucherUsageHistory, VoucherWalletItem } from "../types/customer.types";

export const customerApi = {
  profile: () => apiClient.get<Profile>("/api/v1/profile"),
  updateProfile: (body: Pick<Profile, "fullName" | "phone">) => apiClient.put<Profile>("/api/v1/profile", body),
  vouchers: () => apiClient.get<VoucherWalletItem[]>("/api/v1/cart/vouchers/wallet"),
  voucherHistory: () => apiClient.get<VoucherUsageHistory[]>("/api/v1/cart/vouchers/history"),
  users: (query = "", status = "", role = "") => apiClient.get<PageResult<AdminUser>>(`/api/v1/admin/users?query=${encodeURIComponent(query)}${status ? `&status=${encodeURIComponent(status)}` : ""}${role ? `&role=${encodeURIComponent(role)}` : ""}&page=0&size=100`),
  user: (id: string) => apiClient.get<AdminUser>(`/api/v1/admin/users/${id}`),
  manageUser: (id: string, body: { role?: string; status?: string; reason: string }) => apiClient.patch<AdminUser>(`/api/v1/admin/users/${id}`, body, { headers: { "Idempotency-Key": crypto.randomUUID() } }),
};
