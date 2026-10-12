import type { ProductVariant } from "../types";

export type VariantSelections = Record<string, string>;

export interface VariantSelectionGroup {
  code: string;
  name: string;
  values: string[];
  labels: Record<string, string>;
}

export function variantValue(value: unknown): string {
  if (Array.isArray(value)) return value.map(String).join(", ");
  if (typeof value === "boolean") return value ? "Có" : "Không";
  return value === null || value === undefined ? "" : String(value);
}

export function attributeValue(variant: ProductVariant, code: string): string | undefined {
  const value = variant.attributes.find((attribute) => attribute.attributeCode === code)?.value;
  return value === undefined ? undefined : variantValue(value);
}

export function buildVariantSelectionGroups(variants: ProductVariant[]): VariantSelectionGroup[] {
  const groups = new Map<string, { name: string; values: string[]; labels: Record<string, string> }>();
  for (const variant of variants) {
    for (const attribute of variant.attributes) {
      const current = groups.get(attribute.attributeCode) ?? {
        name: attribute.attributeName,
        values: [],
        labels: {},
      };
      const value = variantValue(attribute.value);
      if (value && !current.values.includes(value)) current.values.push(value);
      if (value) current.labels[value] = attribute.displayValue || value;
      groups.set(attribute.attributeCode, current);
    }
  }
  return [...groups.entries()].map(([code, group]) => ({ code, ...group }));
}

export function selectionsForVariant(variant: ProductVariant | undefined): VariantSelections {
  return Object.fromEntries(
    variant?.attributes.map((attribute) => [attribute.attributeCode, variantValue(attribute.value)]) ?? [],
  );
}

export function singleValueSelections(groups: VariantSelectionGroup[]): VariantSelections {
  return Object.fromEntries(
    groups.filter((group) => group.values.length === 1).map((group) => [group.code, group.values[0]]),
  );
}

export function variantsMatchingSelections(
  variants: ProductVariant[],
  selections: VariantSelections,
): ProductVariant[] {
  const selected = Object.entries(selections).filter(([, value]) => Boolean(value));
  return variants.filter((variant) => selected.every(
    ([code, value]) => attributeValue(variant, code) === value,
  ));
}

export function exactVariantForSelections(
  variants: ProductVariant[],
  groups: VariantSelectionGroup[],
  selections: VariantSelections,
): ProductVariant | undefined {
  if (groups.some((group) => !selections[group.code])) return undefined;
  const matches = variantsMatchingSelections(variants, selections);
  return matches.find((variant) => variant.purchasable) ?? matches[0];
}

export function optionAvailability(
  variants: ProductVariant[],
  selections: VariantSelections,
  code: string,
  value: string,
) {
  const matches = variantsMatchingSelections(variants, { ...selections, [code]: value });
  return {
    exists: matches.length > 0,
    purchasable: matches.some((variant) => variant.purchasable),
  };
}

export function selectionAfterChoosingOption(
  variants: ProductVariant[],
  selections: VariantSelections,
  code: string,
  value: string,
) {
  const requested = { ...selections, [code]: value };
  if (variantsMatchingSelections(variants, requested).length > 0) {
    return { selections: requested, clearedIncompatible: false };
  }
  return {
    selections: { [code]: value },
    clearedIncompatible: Object.entries(selections).some(
      ([selectedCode, selectedValue]) => selectedCode !== code && Boolean(selectedValue),
    ),
  };
}
