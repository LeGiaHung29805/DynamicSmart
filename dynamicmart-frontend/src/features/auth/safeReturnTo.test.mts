import assert from "node:assert/strict";
import test from "node:test";
import { purchaseResumeState, safeReturnTo } from "./safeReturnTo.ts";

const NOW = 1_800_000_000_000;

test("safeReturnTo only accepts local application paths", () => {
  assert.equal(safeReturnTo("https://evil.example/products", NOW), "/");
  assert.equal(safeReturnTo("//evil.example/products", NOW), "/");
  assert.equal(safeReturnTo("/products\\evil", NOW), "/");
});

test("safeReturnTo preserves a valid Buy Now resume state", () => {
  const target = `/products/dynamic-phone-pro?variant=variant-1&quantity=2&resumeUntil=${NOW + 900_000}`;

  assert.equal(safeReturnTo(target, NOW), target);
});

test("safeReturnTo removes Buy Now data after 15 minute deadline", () => {
  const target = `/products/dynamic-phone-pro?variant=variant-1&quantity=2&resumeUntil=${NOW - 1}`;

  assert.equal(safeReturnTo(target, NOW), "/products/dynamic-phone-pro");
});

test("purchaseResumeState restores exact variant and quantity before deadline", () => {
  assert.deepEqual(purchaseResumeState({
    variant: "variant-1",
    quantity: "3",
    resumeUntil: String(NOW + 900_000),
  }, NOW), { variantId: "variant-1", quantity: 3 });
});

test("purchaseResumeState discards expired or invalid values", () => {
  assert.deepEqual(purchaseResumeState({
    variant: "variant-1",
    quantity: "3",
    resumeUntil: String(NOW - 1),
  }, NOW), { quantity: 1 });
  assert.deepEqual(purchaseResumeState({ quantity: "not-a-number" }, NOW), {
    variantId: undefined,
    quantity: 1,
  });
});
