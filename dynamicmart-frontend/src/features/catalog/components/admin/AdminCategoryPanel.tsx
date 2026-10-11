"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { CornerDownRight, FolderTree, Pencil, Plus, RefreshCw, Search } from "lucide-react";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { useToast } from "@/components/ui/Toast";
import {
  createCategory,
  listAdminCategories,
  setCategoryActive,
  updateCategory,
  type CategoryInput,
} from "../../api/admin-catalog.api";
import type { CategoryAdmin } from "../../types";
import { catalogStatusLabel } from "../../utils/catalog-format";
import { errorMessage, fieldClass, Label, textAreaClass } from "./form-utils";

const emptyDraft: CategoryInput = { parentId: null, code: "", name: "", slug: "", description: "", sortOrder: 0 };

interface CategoryRow {
  category: CategoryAdmin;
  depth: number;
  path: string;
  childCount: number;
}

function compareCategories(left: CategoryAdmin, right: CategoryAdmin) {
  return left.sortOrder - right.sortOrder || left.name.localeCompare(right.name, "vi");
}

function buildCategoryRows(categories: CategoryAdmin[]): CategoryRow[] {
  const categoryIds = new Set(categories.map((category) => category.id));
  const childrenByParent = new Map<string | null, CategoryAdmin[]>();

  for (const category of categories) {
    const parentKey = category.parentId && categoryIds.has(category.parentId) ? category.parentId : null;
    const siblings = childrenByParent.get(parentKey) ?? [];
    siblings.push(category);
    childrenByParent.set(parentKey, siblings);
  }

  for (const siblings of childrenByParent.values()) siblings.sort(compareCategories);

  const rows: CategoryRow[] = [];
  const visited = new Set<string>();
  const visit = (category: CategoryAdmin, depth: number, ancestors: string[]) => {
    if (visited.has(category.id)) return;
    visited.add(category.id);
    const children = childrenByParent.get(category.id) ?? [];
    const names = [...ancestors, category.name];
    rows.push({ category, depth, path: names.join(" / "), childCount: children.length });
    for (const child of children) visit(child, depth + 1, names);
  };

  for (const root of childrenByParent.get(null) ?? []) visit(root, 0, []);
  for (const category of [...categories].sort(compareCategories)) visit(category, 0, []);
  return rows;
}

function collectUnavailableParentIds(categories: CategoryAdmin[], editingId?: string) {
  const unavailable = new Set<string>();
  if (!editingId) return unavailable;

  unavailable.add(editingId);
  const queue = [editingId];
  while (queue.length) {
    const current = queue.shift();
    for (const category of categories) {
      if (category.parentId === current && !unavailable.has(category.id)) {
        unavailable.add(category.id);
        queue.push(category.id);
      }
    }
  }
  return unavailable;
}

function toSlug(value: string) {
  return value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function toCode(value: string) {
  return toSlug(value).replace(/-/g, "_").toUpperCase();
}

export function AdminCategoryPanel() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [draft, setDraft] = useState<CategoryInput>(emptyDraft);
  const [editingId, setEditingId] = useState<string>();
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | CategoryAdmin["status"]>("ALL");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const categoryRows = useMemo(() => buildCategoryRows(categories), [categories]);
  const unavailableParentIds = useMemo(
    () => collectUnavailableParentIds(categories, editingId),
    [categories, editingId],
  );
  const visibleRows = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    return categoryRows.filter(({ category, path }) => {
      const matchesStatus = statusFilter === "ALL" || category.status === statusFilter;
      const searchable = `${path} ${category.code} ${category.slug}`.toLocaleLowerCase("vi");
      return matchesStatus && (!normalizedQuery || searchable.includes(normalizedQuery));
    });
  }, [categoryRows, query, statusFilter]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(undefined);
      setCategories(await listAdminCategories());
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải danh mục."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Initial API synchronization is intentionally started after the client mounts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  function reset() {
    setEditingId(undefined);
    setDraft(emptyDraft);
  }

  function edit(category: CategoryAdmin) {
    setEditingId(category.id);
    setDraft({
      parentId: category.parentId ?? null,
      code: category.code,
      name: category.name,
      slug: category.slug,
      description: category.description ?? "",
      sortOrder: category.sortOrder,
    });
  }

  function suggestIdentifiers() {
    setDraft((current) => ({
      ...current,
      code: current.code || toCode(current.name),
      slug: current.slug || toSlug(current.name),
    }));
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true);
      setError(undefined);
      const saved = editingId ? await updateCategory(editingId, draft) : await createCategory(draft);
      setCategories((current) => editingId
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      showToast(editingId ? "Đã cập nhật danh mục." : "Đã tạo danh mục.", "success");
      reset();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  async function toggle(category: CategoryAdmin) {
    try {
      setBusy(true);
      const saved = await setCategoryActive(category.id, category.status !== "ACTIVE");
      setCategories((current) => current.map((item) => item.id === saved.id ? saved : item));
      showToast("Đã cập nhật trạng thái danh mục.", "success");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(320px,.7fr)]">
      <SurfacePanel>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-black">Cây danh mục</h2>
            <p className="mt-1 text-sm text-slate-500">Nên giữ tối đa 2 cấp để khách hàng dễ tìm; dùng trạng thái thay vì xóa cứng.</p>
          </div>
          <button aria-label="Tải lại danh mục" className="grid size-10 place-items-center rounded-xl border border-slate-200 transition hover:bg-slate-50" onClick={() => void load()} type="button">
            <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>

        {error ? <p className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}

        <div className="mt-5 grid gap-3 sm:grid-cols-[minmax(0,1fr)_180px]">
          <label className="relative">
            <span className="sr-only">Tìm danh mục</span>
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
            <input className={`${fieldClass} pl-9`} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm theo tên, mã hoặc slug..." type="search" value={query} />
          </label>
          <label>
            <span className="sr-only">Lọc trạng thái</span>
            <select className={fieldClass} onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)} value={statusFilter}>
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Đang ẩn</option>
            </select>
          </label>
        </div>

        <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
          <span>{visibleRows.length} / {categories.length} danh mục</span>
          <span>Thứ tự: cấp cha → cấp con</span>
        </div>

        <div className="mt-3 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-[760px] w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Danh mục</th>
                <th className="px-4 py-3">Mã / slug</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {visibleRows.map(({ category, childCount, depth, path }) => (
                <tr className={editingId === category.id ? "bg-emerald-50/70" : "hover:bg-slate-50/70"} key={category.id}>
                  <td className="px-4 py-3">
                    <div className="flex items-start gap-2" style={{ paddingLeft: `${Math.min(depth, 4) * 20}px` }}>
                      {depth ? <CornerDownRight className="mt-0.5 size-4 shrink-0 text-slate-400" /> : <FolderTree className="mt-0.5 size-4 shrink-0 text-brand" />}
                      <div className="min-w-0">
                        <p className="font-bold text-slate-900">{category.name}</p>
                        <p className="mt-0.5 truncate text-xs text-slate-500" title={path}>
                          {depth ? path : `${childCount} danh mục con`} · Thứ tự {category.sortOrder}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-mono text-xs font-semibold text-slate-700">{category.code}</p>
                    <p className="mt-0.5 text-xs text-slate-500">/{category.slug}</p>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge label={catalogStatusLabel(category.status)} tone={category.status === "ACTIVE" ? "success" : "neutral"} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button aria-label={`Sửa danh mục ${category.name}`} className="rounded-lg border border-slate-200 p-2 transition hover:bg-white" onClick={() => edit(category)} type="button">
                        <Pencil className="size-4" />
                      </button>
                      <button className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold transition hover:bg-white disabled:opacity-50" disabled={busy} onClick={() => void toggle(category)} type="button">
                        {category.status === "ACTIVE" ? "Ẩn" : "Kích hoạt"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading ? <p className="p-8 text-center text-sm text-slate-500">Đang tải danh mục...</p> : null}
          {!loading && !visibleRows.length ? <p className="p-8 text-center text-sm text-slate-500">Không tìm thấy danh mục phù hợp.</p> : null}
        </div>
      </SurfacePanel>

      <SurfacePanel className="xl:sticky xl:top-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-black tracking-wider text-brand uppercase">{editingId ? "Chỉnh sửa" : "Tạo mới"}</p>
            <h2 className="mt-1 text-xl font-black">Thông tin danh mục</h2>
          </div>
          {editingId ? <button className="text-sm font-bold text-slate-500 hover:text-slate-800" onClick={reset} type="button">Hủy sửa</button> : <Plus className="size-5 text-brand" />}
        </div>

        <form className="mt-5 space-y-4" onSubmit={save}>
          <label>
            <Label>Tên danh mục</Label>
            <input className={fieldClass} maxLength={200} onBlur={suggestIdentifiers} onChange={(event) => setDraft({ ...draft, name: event.target.value })} placeholder="Ví dụ: Điện tử & Công nghệ" required value={draft.name} />
          </label>
          <label>
            <Label>Danh mục cha</Label>
            <select className={fieldClass} onChange={(event) => setDraft({ ...draft, parentId: event.target.value || null })} value={draft.parentId ?? ""}>
              <option value="">Không có — đây là danh mục gốc</option>
              {categoryRows
                .filter(({ category }) => !unavailableParentIds.has(category.id))
                .map(({ category, depth, path }) => <option key={category.id} value={category.id}>{`${"— ".repeat(depth)}${path}`}</option>)}
            </select>
            <span className="mt-1.5 block text-xs text-slate-500">Chọn danh mục cha để tạo nhánh con; không chọn nếu đây là nhóm chính.</span>
          </label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label>
              <Label>Mã</Label>
              <input className={fieldClass} maxLength={80} onChange={(event) => setDraft({ ...draft, code: event.target.value.toUpperCase() })} pattern="[A-Za-z0-9_-]+" placeholder="DIEN_TU" required value={draft.code} />
            </label>
            <label>
              <Label>Thứ tự</Label>
              <input className={fieldClass} min="0" onChange={(event) => setDraft({ ...draft, sortOrder: Number(event.target.value) })} required type="number" value={draft.sortOrder} />
            </label>
          </div>
          <label>
            <Label>Slug</Label>
            <input className={fieldClass} maxLength={220} onChange={(event) => setDraft({ ...draft, slug: event.target.value.toLowerCase() })} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" placeholder="dien-tu" required value={draft.slug} />
          </label>
          <label>
            <Label>Mô tả</Label>
            <textarea className={textAreaClass} maxLength={5000} onChange={(event) => setDraft({ ...draft, description: event.target.value })} placeholder="Mô tả ngắn giúp khách hàng hiểu nhóm sản phẩm." value={draft.description ?? ""} />
          </label>
          <button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white transition hover:bg-emerald-900 disabled:opacity-50" disabled={busy} type="submit">
            {busy ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Tạo danh mục"}
          </button>
        </form>
      </SurfacePanel>
    </div>
  );
}
