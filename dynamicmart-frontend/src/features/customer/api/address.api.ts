import { apiClient } from "@/lib/api/client";
import type { Address, AddressInput } from "../types/customer.types";

/** Canonical customer-address API shared by the address book and Checkout. */
export const addressApi = {
  list: () => apiClient.get<Address[]>("/api/v1/addresses"),
  create: (body: AddressInput) => apiClient.post<Address>("/api/v1/addresses", body),
  update: (id: string, body: AddressInput) => apiClient.put<Address>(`/api/v1/addresses/${id}`, body),
  makeDefault: (id: string) => apiClient.post<Address>(`/api/v1/addresses/${id}/default`),
  deactivate: (id: string) => apiClient.delete<void>(`/api/v1/addresses/${id}`),
};
