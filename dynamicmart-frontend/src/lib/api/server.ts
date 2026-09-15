import "server-only";
import { cookies } from "next/headers";
import { getInternalApiBaseUrl } from "./config";
import { apiRequest, type ApiRequestOptions } from "./request";

export async function serverApiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const cookieStore = await cookies();
  const headers = new Headers(options.headers);
  const cookieHeader = cookieStore.toString();
  if (cookieHeader) headers.set("Cookie", cookieHeader);

  return apiRequest<T>(path, {
    ...options,
    baseUrl: getInternalApiBaseUrl(),
    cache: "no-store",
    headers,
  });
}
