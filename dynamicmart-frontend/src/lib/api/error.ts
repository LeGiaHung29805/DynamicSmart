import type { ApiErrorPayload, ApiValidationError } from "@/contracts/api/common";

export class ApiError extends Error {
  readonly code?: string;
  readonly fields: ApiValidationError[];
  readonly status: number;
  readonly traceId?: string;

  constructor(status: number, payload: ApiErrorPayload = {}) {
    super(payload.message ?? "Yêu cầu không thành công.");
    this.name = "ApiError";
    this.status = status;
    this.code = payload.code;
    this.fields = payload.errors ?? [];
    this.traceId = payload.traceId;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
