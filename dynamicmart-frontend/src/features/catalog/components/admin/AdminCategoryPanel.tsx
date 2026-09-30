"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Pencil, Plus, RefreshCw } from "lucide-react";
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
import { errorMessage, fieldClass, Label, textAreaClass } from "./form-utils";

const emptyDraft: CategoryInput = { parentId: null, code: "", name: "", slug: "", description: "", sortOrder: 0 };

export function AdminCategoryPanel() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState<CategoryAdmin[]>([]);
  const [draft, setDraft] = useState<CategoryInput>(emptyDraft);
  const [editingId, setEditingId] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      setLoading(true); setError(undefined);
      setCategories(await listAdminCategories());
    } catch (caught) {
      setError(errorMessage(caught, "Không thể tải danh mục."));
    } finally { setLoading(false); }
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

  async function save(event: FormEvent) {
    event.preventDefault();
    try {
      setBusy(true); setError(undefined);
      const saved = editingId ? await updateCategory(editingId, draft) : await createCategory(draft);
      setCategories((current) => editingId
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      showToast(editingId ? "Đã cập nhật danh mục." : "Đã tạo danh mục.", "success");
      reset();
    } catch (caught) { setError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  async function toggle(category: CategoryAdmin) {
    try {
      setBusy(true);
      const saved = await setCategoryActive(category.id, category.status !== "ACTIVE");
      setCategories((current) => current.map((item) => item.id === saved.id ? saved : item));
      showToast("Đã cập nhật trạng thái danh mục.", "success");
    } catch (caught) { setError(errorMessage(caught)); }
    finally { setBusy(false); }
  }

  return (
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(320px,.7fr)]">
      <SurfacePanel>
        <div className="flex items-center justify-between gap-3"><div><h2 className="text-xl font-black">Cây danh mục</h2><p className="mt-1 text-sm text-slate-500">Không xóa cứng; dùng trạng thái để ngừng hiển thị.</p></div><button aria-label="Tải lại" className="grid size-10 place-items-center rounded-xl border border-slate-200" onClick={() => void load()} type="button"><RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} /></button></div>
        {error ? <p className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">{error}</p> : null}
        <div className="mt-5 overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Danh mục</th><th className="px-4 py-3">Mã / slug</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Thao tác</th></tr></thead>
          <tbody className="divide-y divide-slate-100">{categories.map((category) => <tr key={category.id}><td className="px-4 py-3"><p className="font-bold text-slate-900">{category.name}</p><p className="text-xs text-slate-500">Thứ tự {category.sortOrder}{category.parentId ? " · Danh mục con" : " · Danh mục gốc"}</p></td><td className="px-4 py-3"><p className="font-mono text-xs">{category.code}</p><p className="text-xs text-slate-500">/{category.slug}</p></td><td className="px-4 py-3"><StatusBadge label={category.status} tone={category.status === "ACTIVE" ? "success" : "neutral"} /></td><td className="px-4 py-3"><div className="flex justify-end gap-2"><button className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50" onClick={() => edit(category)} type="button"><Pencil className="size-4" /></button><button className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-bold" disabled={busy} onClick={() => void toggle(category)} type="button">{category.status === "ACTIVE" ? "Ẩn" : "Kích hoạt"}</button></div></td></tr>)}</tbody></table>
          {!loading && !categories.length ? <p className="p-8 text-center text-sm text-slate-500">Chưa có danh mục.</p> : null}
        </div>
      </SurfacePanel>

      <SurfacePanel className="xl:sticky xl:top-6">
        <div className="flex items-center justify-between"><div><p className="text-xs font-black tracking-wider text-brand uppercase">{editingId ? "Chỉnh sửa" : "Tạo mới"}</p><h2 className="mt-1 text-xl font-black">Thông tin danh mục</h2></div>{editingId ? <button className="text-sm font-bold text-slate-500" onClick={reset} type="button">Hủy sửa</button> : <Plus className="size-5 text-brand" />}</div>
        <form className="mt-5 space-y-4" onSubmit={save}>
          <label><Label>Danh mục cha</Label><select className={fieldClass} onChange={(event) => setDraft({ ...draft, parentId: event.target.value || null })} value={draft.parentId ?? ""}><option value="">Không có</option>{categories.filter((item) => item.id !== editingId).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          <div className="grid gap-4 sm:grid-cols-2"><label><Label>Mã</Label><input className={fieldClass} maxLength={80} onChange={(event) => setDraft({ ...draft, code: event.target.value })} pattern="[A-Za-z0-9_-]+" required value={draft.code} /></label><label><Label>Thứ tự</Label><input className={fieldClass} min="0" onChange={(event) => setDraft({ ...draft, sortOrder: Number(event.target.value) })} required type="number" value={draft.sortOrder} /></label></div>
          <label><Label>Tên danh mục</Label><input className={fieldClass} maxLength={200} onChange={(event) => setDraft({ ...draft, name: event.target.value })} required value={draft.name} /></label>
          <label><Label>Slug</Label><input className={fieldClass} maxLength={220} onChange={(event) => setDraft({ ...draft, slug: event.target.value })} pattern="[a-z0-9]+(?:-[a-z0-9]+)*" required value={draft.slug} /></label>
          <label><Label>Mô tả</Label><textarea className={textAreaClass} maxLength={5000} onChange={(event) => setDraft({ ...draft, description: event.target.value })} value={draft.description ?? ""} /></label>
          <button className="min-h-11 w-full rounded-xl bg-emerald-950 px-4 text-sm font-black text-white disabled:opacity-50" disabled={busy} type="submit">{busy ? "Đang lưu..." : editingId ? "Lưu thay đổi" : "Tạo danh mục"}</button>
        </form>
      </SurfacePanel>
    </div>
  );
}
