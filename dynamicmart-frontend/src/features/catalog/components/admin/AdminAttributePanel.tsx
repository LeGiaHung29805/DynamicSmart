"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Link2, Pencil, Plus, RefreshCw, Unlink } from "lucide-react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import {
  createAttribute,
  createAttributeOption,
  listAdminAttributes,
  listAdminCategories,
  listCategoryAttributes,
  mapCategoryAttribute,
  setAttributeActive,
  setAttributeOptionActive,
  unmapCategoryAttribute,
  updateAttribute,
  updateAttributeOption,
  type AttributeInput,
  type AttributeOptionInput,
  type CategoryAttributeInput,
} from "../../api/admin-catalog.api";
import type {
  AttributeAdmin,
  AttributeDataType,
  AttributeOptionAdmin,
  CategoryAdmin,
  CategoryAttributeAdmin,
} from "../../types";
import { errorMessage, fieldClass, Label, parseJsonObject, textAreaClass } from "./form-utils";

interface AttributeDraft {
  code: string;
  name: string;
  dataType: AttributeDataType;
  validationConfig: string;
}

interface OptionDraft extends AttributeOptionInput {
  id?: string;
}

type AttributeSection = "brands" | "attributes" | "mapping";

const emptyAttribute: AttributeDraft = { code: "", name: "", dataType: "TEXT", validationConfig: "" };
const brandAttribute: AttributeDraft = { code: "BRAND", name: "Thương hiệu", dataType: "SELECT", validationConfig: "" };
const emptyOption: OptionDraft = { code: "", label: "", sortOrder: 0 };
const emptyMapping: CategoryAttributeInput = {
  attributeId: "",
  appliesTo: "PRODUCT",
  required: false,
  filterable: false,
  sortOrder: 0,
};
const dataTypes: AttributeDataType[] = ["TEXT", "NUMBER", "DECIMAL", "BOOLEAN", "SELECT", "MULTI_SELECT"];

function isBrand(attribute: AttributeAdmin) {
  return attribute.code.trim().toUpperCase() === "BRAND";
}

export function AdminAttributePanel() {
  const { showToast } = useToast();
  const [section, setSection] = useState<AttributeSection>("brands");
  const [attributes, setAttributes] = useState<AttributeAdmin[]>([]);
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [selectedAttributeId, setSelectedAttributeId] = useState<string>();
  const [selectedCategoryId, setSelectedCategoryId] = useState("");
  const [mappings, setMappings] = useState<CategoryAttributeAdmin[]>([]);
  const [attributeDraft, setAttributeDraft] = useState<AttributeDraft>(brandAttribute);
  const [editingAttributeId, setEditingAttributeId] = useState<string>();
  const [optionDraft, setOptionDraft] = useState<OptionDraft>(emptyOption);
  const [mappingDraft, setMappingDraft] = useState<CategoryAttributeInput>(emptyMapping);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const brandCount = useMemo(() => attributes.filter(isBrand).length, [attributes]);
  const standardAttributeCount = attributes.length - brandCount;
  const visibleAttributes = useMemo(
    () => attributes.filter((attribute) => section === "brands" ? isBrand(attribute) : !isBrand(attribute)),
    [attributes, section],
  );
  const selectedAttribute = useMemo(
    () => attributes.find((attribute) => attribute.id === selectedAttributeId),
    [attributes, selectedAttributeId],
  );

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      const [attributeItems, categoryItems] = await Promise.all([listAdminAttributes(), listAdminCategories()]);
      const preferredAttribute = attributeItems.find(isBrand) ?? attributeItems[0];
      setAttributes(attributeItems);
      setCategories(categoryItems);
      setSelectedAttributeId((current) =>
        current && attributeItems.some((item) => item.id === current) ? current : preferredAttribute?.id,
      );
      setSelectedCategoryId((current) => current || categoryItems[0]?.id || "");
      setMappingDraft((current) => ({
        ...current,
        attributeId: current.attributeId || preferredAttribute?.id || "",
      }));
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải cấu hình thuộc tính."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  useEffect(() => {
    let active = true;
    if (!selectedCategoryId) return;
    listCategoryAttributes(selectedCategoryId)
      .then((items) => {
        if (active) setMappings(items);
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught, "Không thể tải ánh xạ thuộc tính."));
      });
    return () => {
      active = false;
    };
  }, [selectedCategoryId]);

  function chooseAttribute(attribute: AttributeAdmin) {
    setSelectedAttributeId(attribute.id);
    setOptionDraft(emptyOption);
    setMappingDraft((current) => ({ ...current, attributeId: attribute.id }));
  }

  function changeSection(nextSection: AttributeSection) {
    setSection(nextSection);
    setEditingAttributeId(undefined);
    setAttributeDraft(nextSection === "brands" ? brandAttribute : emptyAttribute);
    setOptionDraft(emptyOption);

    if (nextSection === "mapping") return;
    const firstVisible = attributes.find((attribute) => nextSection === "brands" ? isBrand(attribute) : !isBrand(attribute));
    setSelectedAttributeId(firstVisible?.id);
    if (firstVisible) {
      setMappingDraft((current) => ({ ...current, attributeId: firstVisible.id }));
    }
  }

  function resetAttribute() {
    setEditingAttributeId(undefined);
    setAttributeDraft(section === "brands" ? brandAttribute : emptyAttribute);
  }

  function editAttribute(attribute: AttributeAdmin) {
    setEditingAttributeId(attribute.id);
    setAttributeDraft({
      code: attribute.code,
      name: attribute.name,
      dataType: attribute.dataType,
      validationConfig: attribute.validationConfig ? JSON.stringify(attribute.validationConfig, null, 2) : "",
    });
  }

  async function saveAttribute(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true);
      setError(undefined);
      const input: AttributeInput = {
        code: section === "brands" ? "BRAND" : attributeDraft.code,
        name: attributeDraft.name,
        dataType: section === "brands" ? "SELECT" : attributeDraft.dataType,
        validationConfig: parseJsonObject(attributeDraft.validationConfig),
      };
      const saved = editingAttributeId
        ? await updateAttribute(editingAttributeId, input)
        : await createAttribute(input);
      setAttributes((current) => editingAttributeId
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      chooseAttribute(saved);
      showToast(editingAttributeId ? "Đã cập nhật cấu hình." : "Đã tạo cấu hình.", "success");
      resetAttribute();
    } catch (caught) {
      setError(errorMessage(caught, "Cấu hình JSON không hợp lệ."));
    } finally {
      setBusy(false);
    }
  }

  async function toggleAttribute(attribute: AttributeAdmin) {
    try {
      setBusy(true);
      const saved = await setAttributeActive(attribute.id, attribute.status !== "ACTIVE");
      setAttributes((current) => current.map((item) => item.id === saved.id ? saved : item));
      showToast("Đã cập nhật trạng thái.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  function editOption(option: AttributeOptionAdmin) {
    setOptionDraft({ id: option.id, code: option.code, label: option.label, sortOrder: option.sortOrder });
  }

  async function saveOption(event: FormEvent) {
    event.preventDefault();
    if (!selectedAttribute) return;
    try {
      setBusy(true);
      setError(undefined);
      const input: AttributeOptionInput = {
        code: optionDraft.code,
        label: optionDraft.label,
        sortOrder: optionDraft.sortOrder,
      };
      const saved = optionDraft.id
        ? await updateAttributeOption(selectedAttribute.id, optionDraft.id, input)
        : await createAttributeOption(selectedAttribute.id, input);
      setAttributes((current) => current.map((attribute) => attribute.id !== selectedAttribute.id ? attribute : {
        ...attribute,
        options: optionDraft.id
          ? attribute.options.map((option) => option.id === saved.id ? saved : option)
          : [...attribute.options, saved],
      }));
      setOptionDraft(emptyOption);
      showToast(optionDraft.id ? "Đã cập nhật lựa chọn." : "Đã thêm lựa chọn.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function toggleOption(option: AttributeOptionAdmin) {
    if (!selectedAttribute) return;
    try {
      setBusy(true);
      const saved = await setAttributeOptionActive(selectedAttribute.id, option.id, option.status !== "ACTIVE");
      setAttributes((current) => current.map((attribute) => attribute.id !== selectedAttribute.id ? attribute : {
        ...attribute,
        options: attribute.options.map((item) => item.id === saved.id ? saved : item),
      }));
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function saveMapping(event: FormEvent) {
    event.preventDefault();
    if (!selectedCategoryId || !mappingDraft.attributeId) return;
    try {
      setBusy(true);
      setError(undefined);
      const saved = await mapCategoryAttribute(selectedCategoryId, mappingDraft);
      setMappings((current) => current.some((item) => item.attributeId === saved.attributeId)
        ? current.map((item) => item.attributeId === saved.attributeId ? saved : item)
        : [...current, saved]);
      showToast("Đã lưu ánh xạ thuộc tính cho danh mục.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function removeMapping(mapping: CategoryAttributeAdmin) {
    if (!selectedCategoryId) return;
    try {
      setBusy(true);
      await unmapCategoryAttribute(selectedCategoryId, mapping.id);
      setMappings((current) => current.filter((item) => item.id !== mapping.id));
      showToast("Đã gỡ thuộc tính khỏi danh mục.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  const brandAlreadyConfigured = section === "brands" && brandCount > 0 && !editingAttributeId;

  return (
    <div className="space-y-6">
      {error ? (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p>
      ) : null}

      <div className="flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
        <SectionButton
          active={section === "brands"}
          count={brandCount}
          label="Thương hiệu"
          onClick={() => changeSection("brands")}
        />
        <SectionButton
          active={section === "attributes"}
          count={standardAttributeCount}
          label="Thuộc tính khác"
          onClick={() => changeSection("attributes")}
        />
        <SectionButton
          active={section === "mapping"}
          count={mappings.length}
          label="Ánh xạ danh mục"
          onClick={() => changeSection("mapping")}
        />
      </div>

      {section === "mapping" ? (
        <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(340px,.85fr)]">
          <SurfacePanel>
            <div>
              <p className="text-xs font-black tracking-wider text-brand uppercase">Schema danh mục</p>
              <h2 className="mt-1 text-xl font-black">Thuộc tính đang áp dụng</h2>
              <p className="mt-1 text-sm text-slate-500">Chọn danh mục để kiểm tra thuộc tính của sản phẩm và biến thể.</p>
            </div>
            <label className="mt-5 block">
              <Label>Danh mục</Label>
              <select
                className={fieldClass}
                onChange={(event) => setSelectedCategoryId(event.target.value)}
                value={selectedCategoryId}
              >
                <option value="">Chọn danh mục</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>{category.name}</option>
                ))}
              </select>
            </label>
            <div className="mt-4 space-y-2">
              {mappings.map((mapping) => (
                <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 p-3" key={mapping.id}>
                  <div>
                    <p className="font-bold">{mapping.attributeName}</p>
                    <p className="text-xs text-slate-500">
                      {mapping.appliesTo} · {mapping.required ? "Bắt buộc" : "Tùy chọn"} · {mapping.filterable ? "Có bộ lọc" : "Không lọc"}
                    </p>
                  </div>
                  <button
                    aria-label="Gỡ ánh xạ"
                    className="rounded-lg border border-red-200 p-2 text-red-700 hover:bg-red-50"
                    disabled={busy}
                    onClick={() => void removeMapping(mapping)}
                    type="button"
                  >
                    <Unlink className="size-4" />
                  </button>
                </div>
              ))}
              {!mappings.length ? (
                <p className="rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Danh mục này chưa có thuộc tính.</p>
              ) : null}
            </div>
          </SurfacePanel>

          <SurfacePanel className="xl:sticky xl:top-40">
            <div>
              <p className="text-xs font-black tracking-wider text-brand uppercase">Thiết lập hiển thị</p>
              <h2 className="mt-1 text-xl font-black">Thêm thuộc tính vào danh mục</h2>
            </div>
            <form className="mt-5 space-y-4" onSubmit={saveMapping}>
              <label>
                <Label>Thuộc tính</Label>
                <select
                  className={fieldClass}
                  onChange={(event) => setMappingDraft({ ...mappingDraft, attributeId: event.target.value })}
                  required
                  value={mappingDraft.attributeId}
                >
                  <option value="">Chọn thuộc tính</option>
                  {attributes.map((attribute) => (
                    <option key={attribute.id} value={attribute.id}>{attribute.name}</option>
                  ))}
                </select>
              </label>
              <div className="grid gap-3 sm:grid-cols-2">
                <label>
                  <Label>Áp dụng cho</Label>
                  <select
                    className={fieldClass}
                    onChange={(event) => setMappingDraft({
                      ...mappingDraft,
                      appliesTo: event.target.value as "PRODUCT" | "VARIANT",
                    })}
                    value={mappingDraft.appliesTo}
                  >
                    <option value="PRODUCT">Sản phẩm</option>
                    <option value="VARIANT">Biến thể</option>
                  </select>
                </label>
                <label>
                  <Label>Thứ tự</Label>
                  <input
                    className={fieldClass}
                    min="0"
                    onChange={(event) => setMappingDraft({ ...mappingDraft, sortOrder: Number(event.target.value) })}
                    type="number"
                    value={mappingDraft.sortOrder}
                  />
                </label>
              </div>
              <div className="flex flex-wrap gap-5 rounded-xl bg-slate-50 p-4 text-sm">
                <label className="flex items-center gap-2">
                  <input
                    checked={mappingDraft.required}
                    onChange={(event) => setMappingDraft({ ...mappingDraft, required: event.target.checked })}
                    type="checkbox"
                  />
                  Bắt buộc nhập
                </label>
                <label className="flex items-center gap-2">
                  <input
                    checked={mappingDraft.filterable}
                    onChange={(event) => setMappingDraft({ ...mappingDraft, filterable: event.target.checked })}
                    type="checkbox"
                  />
                  Dùng làm bộ lọc
                </label>
              </div>
              <button
                className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50"
                disabled={busy || !selectedCategoryId}
                type="submit"
              >
                <Link2 className="size-4" /> Lưu ánh xạ
              </button>
            </form>
          </SurfacePanel>
        </div>
      ) : (
        <>
          <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(340px,.8fr)]">
            <SurfacePanel>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h2 className="text-xl font-black">{section === "brands" ? "Danh sách thương hiệu" : "Thuộc tính sản phẩm"}</h2>
                  <p className="mt-1 text-sm text-slate-500">
                    {section === "brands"
                      ? "Mỗi lựa chọn là một thương hiệu có thể gán cho sản phẩm."
                      : "Kích thước, màu sắc và các thông số động dùng chung."}
                  </p>
                </div>
                <button
                  aria-label="Tải lại"
                  className="grid size-10 place-items-center rounded-xl border border-slate-200 hover:bg-slate-50"
                  onClick={() => void load()}
                  type="button"
                >
                  <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
                </button>
              </div>
              <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">Cấu hình</th>
                      <th className="px-4 py-3">Kiểu</th>
                      <th className="px-4 py-3">Trạng thái</th>
                      <th className="px-4 py-3 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {visibleAttributes.map((attribute) => (
                      <tr className={selectedAttributeId === attribute.id ? "bg-emerald-50/60" : ""} key={attribute.id}>
                        <td className="px-4 py-3">
                          <button className="text-left" onClick={() => chooseAttribute(attribute)} type="button">
                            <span className="block font-bold text-slate-900">{attribute.name}</span>
                            <span className="font-mono text-xs text-slate-500">
                              {attribute.code} · {attribute.options.length} lựa chọn
                            </span>
                          </button>
                        </td>
                        <td className="px-4 py-3 text-xs font-bold">{attribute.dataType}</td>
                        <td className="px-4 py-3">
                          <StatusBadge label={attribute.status} tone={attribute.status === "ACTIVE" ? "success" : "neutral"} />
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex justify-end gap-2">
                            <button
                              aria-label={`Sửa ${attribute.name}`}
                              className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50"
                              onClick={() => editAttribute(attribute)}
                              type="button"
                            >
                              <Pencil className="size-4" />
                            </button>
                            <button
                              className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold"
                              disabled={busy}
                              onClick={() => void toggleAttribute(attribute)}
                              type="button"
                            >
                              {attribute.status === "ACTIVE" ? "Ẩn" : "Bật"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!loading && !visibleAttributes.length ? (
                  <p className="p-8 text-center text-sm text-slate-500">
                    {section === "brands" ? "Chưa có cấu hình thương hiệu." : "Chưa có thuộc tính nào."}
                  </p>
                ) : null}
              </div>
            </SurfacePanel>

            <SurfacePanel className="xl:sticky xl:top-40">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs font-black tracking-wider text-brand uppercase">
                    {editingAttributeId ? "Chỉnh sửa" : "Tạo mới"}
                  </p>
                  <h2 className="mt-1 text-xl font-black">
                    {section === "brands" ? "Cấu hình thương hiệu" : "Cấu hình thuộc tính"}
                  </h2>
                </div>
                {editingAttributeId ? (
                  <button className="text-sm font-bold text-slate-500" onClick={resetAttribute} type="button">Hủy sửa</button>
                ) : (
                  <Plus className="size-5 text-brand" />
                )}
              </div>
              {brandAlreadyConfigured ? (
                <div className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-5">
                  <p className="font-bold text-emerald-950">Cấu hình thương hiệu đã sẵn sàng</p>
                  <p className="mt-1 text-sm leading-6 text-emerald-800">
                    Dự án chỉ cần một thuộc tính BRAND. Bạn có thể quản lý tên thương hiệu ở danh sách lựa chọn bên dưới.
                  </p>
                  <button
                    className="mt-4 min-h-10 rounded-xl border border-emerald-300 bg-white px-4 text-sm font-bold text-emerald-950"
                    onClick={() => editAttribute(visibleAttributes[0])}
                    type="button"
                  >
                    Sửa cấu hình BRAND
                  </button>
                </div>
              ) : <form className="mt-5 space-y-4" onSubmit={saveAttribute}>
                <div className="grid gap-4 sm:grid-cols-2">
                  <label>
                    <Label>Mã</Label>
                    <input
                      className={fieldClass}
                      onChange={(event) => setAttributeDraft({ ...attributeDraft, code: event.target.value })}
                      pattern="[A-Za-z0-9_-]+"
                      readOnly={section === "brands"}
                      required
                      value={attributeDraft.code}
                    />
                  </label>
                  <label>
                    <Label>Kiểu dữ liệu</Label>
                    <select
                      className={fieldClass}
                      disabled={section === "brands"}
                      onChange={(event) => setAttributeDraft({
                        ...attributeDraft,
                        dataType: event.target.value as AttributeDataType,
                      })}
                      value={attributeDraft.dataType}
                    >
                      {dataTypes.map((type) => <option key={type}>{type}</option>)}
                    </select>
                  </label>
                </div>
                <label>
                  <Label>Tên hiển thị</Label>
                  <input
                    className={fieldClass}
                    onChange={(event) => setAttributeDraft({ ...attributeDraft, name: event.target.value })}
                    required
                    value={attributeDraft.name}
                  />
                </label>
                <details className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <summary className="cursor-pointer text-sm font-bold text-slate-700">Thiết lập kiểm tra nâng cao</summary>
                  <label className="mt-4 block">
                    <Label>Validation config (JSON)</Label>
                    <textarea
                      className={`${textAreaClass} font-mono text-xs`}
                      onChange={(event) => setAttributeDraft({ ...attributeDraft, validationConfig: event.target.value })}
                      placeholder={'{"min": 0, "max": 100}'}
                      value={attributeDraft.validationConfig}
                    />
                  </label>
                </details>
                <button
                  className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50"
                  disabled={busy}
                  type="submit"
                >
                  {busy ? "Đang lưu..." : editingAttributeId ? "Lưu thay đổi" : "Tạo cấu hình"}
                </button>
              </form>}
            </SurfacePanel>
          </div>

          <SurfacePanel>
            <div>
              <p className="text-xs font-black tracking-wider text-brand uppercase">Giá trị có thể chọn</p>
              <h2 className="mt-1 text-xl font-black">{selectedAttribute?.name ?? "Chọn một cấu hình"}</h2>
              <p className="mt-1 text-sm text-slate-500">
                {section === "brands"
                  ? "Thêm tên thương hiệu sẽ xuất hiện trong form sản phẩm và bộ lọc cửa hàng."
                  : "Dùng cho thuộc tính SELECT hoặc MULTI_SELECT."}
              </p>
            </div>
            {!selectedAttribute ? (
              <p className="mt-5 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">Chọn một cấu hình để quản lý lựa chọn.</p>
            ) : (
              <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1.15fr)_minmax(340px,.85fr)]">
                <div className="space-y-2">
                  {selectedAttribute.options.map((option) => (
                    <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 p-3" key={option.id}>
                      <div>
                        <p className="font-bold">{option.label}</p>
                        <p className="font-mono text-xs text-slate-500">{option.code} · thứ tự {option.sortOrder}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <StatusBadge label={option.status} tone={option.status === "ACTIVE" ? "success" : "neutral"} />
                        <button
                          aria-label={`Sửa ${option.label}`}
                          className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50"
                          onClick={() => editOption(option)}
                          type="button"
                        >
                          <Pencil className="size-4" />
                        </button>
                        <button
                          className="rounded-lg border border-slate-200 px-2 py-2 text-xs font-bold"
                          onClick={() => void toggleOption(option)}
                          type="button"
                        >
                          {option.status === "ACTIVE" ? "Ẩn" : "Bật"}
                        </button>
                      </div>
                    </div>
                  ))}
                  {!selectedAttribute.options.length ? (
                    <p className="rounded-xl bg-slate-50 p-5 text-center text-sm text-slate-500">Chưa có lựa chọn.</p>
                  ) : null}
                </div>
                <form className="grid content-start gap-3 rounded-2xl bg-slate-50 p-4 sm:grid-cols-2" onSubmit={saveOption}>
                  <label>
                    <Label>Mã lựa chọn</Label>
                    <input
                      className={fieldClass}
                      onChange={(event) => setOptionDraft({ ...optionDraft, code: event.target.value })}
                      pattern="[A-Za-z0-9_-]+"
                      required
                      value={optionDraft.code}
                    />
                  </label>
                  <label>
                    <Label>Thứ tự</Label>
                    <input
                      className={fieldClass}
                      min="0"
                      onChange={(event) => setOptionDraft({ ...optionDraft, sortOrder: Number(event.target.value) })}
                      type="number"
                      value={optionDraft.sortOrder}
                    />
                  </label>
                  <label className="sm:col-span-2">
                    <Label>Tên hiển thị</Label>
                    <input
                      className={fieldClass}
                      onChange={(event) => setOptionDraft({ ...optionDraft, label: event.target.value })}
                      required
                      value={optionDraft.label}
                    />
                  </label>
                  <div className="flex gap-2 sm:col-span-2">
                    <button
                      className="min-h-10 flex-1 rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50"
                      disabled={busy}
                      type="submit"
                    >
                      {optionDraft.id ? "Lưu lựa chọn" : section === "brands" ? "Thêm thương hiệu" : "Thêm lựa chọn"}
                    </button>
                    {optionDraft.id ? (
                      <button
                        className="rounded-xl border border-slate-200 px-4 text-sm font-bold"
                        onClick={() => setOptionDraft(emptyOption)}
                        type="button"
                      >
                        Hủy
                      </button>
                    ) : null}
                  </div>
                </form>
              </div>
            )}
          </SurfacePanel>
        </>
      )}
    </div>
  );
}

function SectionButton({
  active,
  count,
  label,
  onClick,
}: Readonly<{ active: boolean; count: number; label: string; onClick: () => void }>) {
  return (
    <button
      className={`rounded-xl px-4 py-2.5 text-sm font-bold transition ${
        active ? "bg-emerald-950 text-white shadow-sm" : "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
      }`}
      onClick={onClick}
      type="button"
    >
      {label} <span className={active ? "text-emerald-200" : "text-slate-400"}>({count})</span>
    </button>
  );
}
