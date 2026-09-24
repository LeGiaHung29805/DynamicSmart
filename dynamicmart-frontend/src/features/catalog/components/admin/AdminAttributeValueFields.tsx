"use client";

import type { AttributeValueInput } from "../../api/admin-catalog.api";
import type { AttributeAdmin, AttributeAppliesTo, AttributeValue, CategoryAttributeAdmin } from "../../types";
import { fieldClass, Label } from "./form-utils";

export type AttributeValueMap = Record<string, unknown>;

export function attributeMap(values: AttributeValue[]): AttributeValueMap {
  return Object.fromEntries(values.map((value) => [value.attributeId, value.value]));
}

export function attributeInputs(values: AttributeValueMap): AttributeValueInput[] {
  return Object.entries(values)
    .filter(([, value]) => value !== "" && value !== null && value !== undefined && (!Array.isArray(value) || value.length > 0))
    .map(([attributeId, value]) => ({ attributeId, value }));
}

export function AdminAttributeValueFields({
  appliesTo,
  attributes,
  mappings,
  values,
  onChange,
}: Readonly<{
  appliesTo: AttributeAppliesTo;
  attributes: AttributeAdmin[];
  mappings: CategoryAttributeAdmin[];
  values: AttributeValueMap;
  onChange: (values: AttributeValueMap) => void;
}>) {
  const fields = mappings
    .filter((mapping) => mapping.appliesTo === appliesTo)
    .map((mapping) => ({ mapping, attribute: attributes.find((item) => item.id === mapping.attributeId) }))
    .filter((field): field is { mapping: CategoryAttributeAdmin; attribute: AttributeAdmin } => Boolean(field.attribute));

  if (!fields.length) return <p className="rounded-xl bg-slate-50 px-4 py-3 text-xs text-slate-500">Danh mục chưa khai báo thuộc tính {appliesTo.toLowerCase()}.</p>;

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {fields.map(({ attribute, mapping }) => {
        const value = values[attribute.id];
        const label = <>{attribute.name}{mapping.required ? <span className="text-red-600"> *</span> : null}</>;
        if (attribute.dataType === "BOOLEAN") {
          return <label className="flex min-h-10 items-center gap-2 self-end rounded-xl border border-slate-200 px-3 text-sm" key={attribute.id}><input checked={value === true} onChange={(event) => onChange({ ...values, [attribute.id]: event.target.checked })} type="checkbox" />{label}</label>;
        }
        if (attribute.dataType === "SELECT") {
          return <label key={attribute.id}><Label>{label}</Label><select className={fieldClass} onChange={(event) => onChange({ ...values, [attribute.id]: event.target.value })} required={mapping.required} value={typeof value === "string" ? value : ""}><option value="">Chọn...</option>{attribute.options.filter((option) => option.status === "ACTIVE").map((option) => <option key={option.id} value={option.code}>{option.label}</option>)}</select></label>;
        }
        if (attribute.dataType === "MULTI_SELECT") {
          const selected = Array.isArray(value) ? value.map(String) : [];
          return <fieldset className="rounded-xl border border-slate-200 p-3" key={attribute.id}><legend className="px-1 text-sm font-semibold text-slate-700">{label}</legend><div className="flex flex-wrap gap-3">{attribute.options.filter((option) => option.status === "ACTIVE").map((option) => <label className="flex items-center gap-1.5 text-sm" key={option.id}><input checked={selected.includes(option.code)} onChange={(event) => onChange({ ...values, [attribute.id]: event.target.checked ? [...selected, option.code] : selected.filter((code) => code !== option.code) })} type="checkbox" />{option.label}</label>)}</div></fieldset>;
        }
        const numeric = attribute.dataType === "NUMBER" || attribute.dataType === "DECIMAL";
        return <label key={attribute.id}><Label>{label}</Label><input className={fieldClass} onChange={(event) => onChange({ ...values, [attribute.id]: numeric && event.target.value !== "" ? Number(event.target.value) : event.target.value })} required={mapping.required} step={attribute.dataType === "DECIMAL" ? "any" : undefined} type={numeric ? "number" : "text"} value={typeof value === "string" || typeof value === "number" ? value : ""} /></label>;
      })}
    </div>
  );
}
