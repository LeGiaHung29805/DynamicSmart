import { createElement, type ReactNode } from "react";
import type { AttributeValueInput } from "../../api/admin-catalog.api";
import type { AttributeValue } from "../../types";
import { isApiError } from "@/lib/api/error";

export const fieldClass = "min-h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-600/10 disabled:bg-slate-100";
export const textAreaClass = `${fieldClass} min-h-24 py-2.5`;

export function errorMessage(error: unknown, fallback = "Không thể hoàn tất thao tác.") {
  return isApiError(error) ? error.message : error instanceof Error ? error.message : fallback;
}

export function optionalNumber(value: string): number | null {
  return value.trim() ? Number(value) : null;
}

export function parseJsonObject(value: string): unknown {
  return value.trim() ? JSON.parse(value) : null;
}

export function parseAttributeValues(value: string): AttributeValueInput[] {
  if (!value.trim()) return [];
  const parsed: unknown = JSON.parse(value);
  if (!Array.isArray(parsed) || parsed.some((item) => {
    if (!item || typeof item !== "object") return true;
    const candidate = item as Record<string, unknown>;
    return typeof candidate.attributeId !== "string" || !("value" in candidate);
  })) {
    throw new Error("Thuộc tính phải là mảng JSON gồm attributeId và value.");
  }
  return parsed as AttributeValueInput[];
}

export function stringifyAttributeValues(values: AttributeValue[]) {
  return JSON.stringify(values.map(({ attributeId, value }) => ({ attributeId, value })), null, 2);
}

export function toDateTimeLocal(value?: string | null) {
  if (!value) return "";
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function Label({ children }: Readonly<{ children: ReactNode }>) {
  return createElement("span", { className: "mb-1.5 block text-sm font-semibold text-slate-700" }, children);
}
