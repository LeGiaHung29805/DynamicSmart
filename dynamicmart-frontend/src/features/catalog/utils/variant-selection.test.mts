import assert from "node:assert/strict";
import test from "node:test";
import type { ProductVariant } from "../types.ts";
import {
  buildVariantSelectionGroups,
  exactVariantForSelections,
  optionAvailability,
  selectionAfterChoosingOption,
  selectionsForVariant,
} from "./variant-selection.ts";

function variant(id: string, color: string, size: string, purchasable = true): ProductVariant {
  return {
    id,
    productId: "product-1",
    sku: id,
    name: `${color} / ${size}`,
    price: { listPriceVnd: 100_000, directSaleDiscountVnd: 0 },
    weightGrams: 200,
    status: "ACTIVE",
    sortOrder: 0,
    inventory: {
      onHandQuantity: purchasable ? 5 : 0,
      reservedQuantity: 0,
      availableQuantity: purchasable ? 5 : 0,
    },
    purchasable,
    attributes: [
      { id: `${id}-color`, attributeId: "color", attributeCode: "COLOR", attributeName: "Màu sắc", dataType: "SELECT", value: color },
      { id: `${id}-size`, attributeId: "size", attributeCode: "SIZE", attributeName: "Kích cỡ", dataType: "SELECT", value: size },
    ],
    images: [],
  };
}

const variants = [
  variant("black-s", "BLACK", "S"),
  variant("black-m", "BLACK", "M", false),
  variant("white-m", "WHITE", "M"),
];

test("builds one selector per variant attribute", () => {
  assert.deepEqual(buildVariantSelectionGroups(variants), [
    { code: "COLOR", name: "Màu sắc", values: ["BLACK", "WHITE"], labels: { BLACK: "BLACK", WHITE: "WHITE" } },
    { code: "SIZE", name: "Kích cỡ", values: ["S", "M"], labels: { S: "S", M: "M" } },
  ]);
});

test("only resolves a variant after every selector has a value", () => {
  const groups = buildVariantSelectionGroups(variants);
  assert.equal(exactVariantForSelections(variants, groups, { COLOR: "BLACK" }), undefined);
  assert.equal(exactVariantForSelections(variants, groups, { COLOR: "BLACK", SIZE: "S" })?.id, "black-s");
});

test("does not silently replace another selected attribute", () => {
  const groups = buildVariantSelectionGroups(variants);
  const selected = exactVariantForSelections(variants, groups, { COLOR: "BLACK", SIZE: "M" });
  assert.equal(selected?.id, "black-m");
  assert.deepEqual(selectionsForVariant(selected), { COLOR: "BLACK", SIZE: "M" });
});

test("calculates availability against the current partial combination", () => {
  assert.deepEqual(optionAvailability(variants, { COLOR: "BLACK" }, "SIZE", "M"), {
    exists: true,
    purchasable: false,
  });
  assert.deepEqual(optionAvailability(variants, { COLOR: "BLACK" }, "SIZE", "S"), {
    exists: true,
    purchasable: true,
  });
  assert.deepEqual(optionAvailability(variants, { COLOR: "WHITE" }, "SIZE", "S"), {
    exists: false,
    purchasable: false,
  });
});

test("clears incompatible choices instead of silently switching their values", () => {
  assert.deepEqual(selectionAfterChoosingOption(
    variants,
    { COLOR: "BLACK", SIZE: "S" },
    "COLOR",
    "WHITE",
  ), {
    selections: { COLOR: "WHITE" },
    clearedIncompatible: true,
  });
  assert.deepEqual(selectionAfterChoosingOption(
    variants,
    { COLOR: "BLACK" },
    "SIZE",
    "S",
  ), {
    selections: { COLOR: "BLACK", SIZE: "S" },
    clearedIncompatible: false,
  });
});
