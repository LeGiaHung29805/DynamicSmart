import { apiClient } from "@/lib/api/client";

export type LocationOption = { id: number; name: string };

export const locationApi = {
  provinces: () => apiClient.get<LocationOption[]>("api/v1/locations/provinces"),
  wards: (provinceId: number) => apiClient.get<LocationOption[]>(`api/v1/locations/wards?provinceId=${provinceId}`),
};
