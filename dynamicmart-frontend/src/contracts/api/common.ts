export interface ApiPageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  meta?: ApiPageMeta;
  timestamp?: string;
}

export interface ApiValidationError {
  field?: string;
  message: string;
}

export interface ApiErrorPayload {
  code?: string;
  message?: string;
  errors?: ApiValidationError[];
  traceId?: string;
}

export function isApiResponse<T>(payload: unknown): payload is ApiResponse<T> {
  return typeof payload === "object" && payload !== null && "data" in payload;
}
