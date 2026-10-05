"use client";

import { History, Search, ShieldCheck, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";
import { PageHeader } from "@/components/common/PageHeader";
import { SurfacePanel } from "@/components/common/SurfacePanel";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { customerApi } from "../api/customer.api";
import type { AdminUser } from "../types/customer.types";

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [selected, setSelected] = useState<AdminUser | null>(null);
  const [query, setQuery] = useState(""); const [status, setStatus] = useState(""); const [role, setRole] = useState("");
  const [message, setMessage] = useState("Đang tải…");
  const load = () => customerApi.users(query, status, role).then((page) => { setUsers(page.content); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách người dùng."));
  useEffect(() => { const timer = setTimeout(() => { customerApi.users(query, status, role).then((page) => { setUsers(page.content); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách người dùng.")); }, 250); return () => clearTimeout(timer); }, [query, status, role]);
  const manage = async (user: AdminUser, body: { role?: string; status?: string }, action: string) => {
    const reason = window.prompt(`Lý do ${action}:`)?.trim(); if (!reason) return;
    try { await customerApi.manageUser(user.id, { ...body, reason }); load(); if (selected?.id === user.id) setSelected(await customerApi.user(user.id)); }
    catch { setMessage("Không thể cập nhật. Kiểm tra quyền tự thay đổi hoặc quy tắc admin cuối cùng."); }
  };
  const showDetail = async (id: string) => { try { setSelected(await customerApi.user(id)); } catch { setMessage("Không thể tải chi tiết người dùng."); } };

  return <div className="space-y-7"><PageHeader eyebrow="Quản lý tài khoản" title="Người dùng" description="Tìm kiếm, phân quyền, khóa và vô hiệu hóa tài khoản. Mọi thay đổi đều lưu lý do và lịch sử kiểm toán." />
    <div className="grid gap-4 sm:grid-cols-3">
      <Summary icon={UsersRound} label="Tài khoản hiển thị" value={users.length} />
      <Summary icon={ShieldCheck} label="Đang hoạt động" value={users.filter((user) => user.status === "ACTIVE").length} />
      <Summary icon={ShieldCheck} label="Quản trị viên" value={users.filter((user) => user.role === "ADMIN").length} />
    </div>
    <SurfacePanel className="overflow-hidden p-0"><div className="border-b border-stone-200 bg-stone-50/80 p-5"><div className="mb-4 flex items-center gap-2"><Search className="h-5 w-5 text-rose-600" /><h2 className="font-black text-stone-900">Bộ lọc tài khoản</h2></div><div className="grid gap-4 md:grid-cols-[1fr_180px_180px]"><Input label="Tìm kiếm" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc email" /><Select label="Vai trò" value={role} onChange={(event) => setRole(event.target.value)}><option value="">Tất cả</option><option value="CUSTOMER">Khách hàng</option><option value="ADMIN">Quản trị viên</option></Select><Select label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="DISABLED">Vô hiệu hóa</option></Select></div>
      {message ? <p className={`mt-4 text-sm ${message.startsWith("Không") ? "text-red-600" : "text-stone-500"}`}>{message}</p> : null}</div>
      {users.length === 0 && !message ? <p className="px-5 py-14 text-center text-sm text-stone-500">Không tìm thấy tài khoản phù hợp.</p> : <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white text-xs uppercase tracking-wide text-stone-500"><tr><th className="px-5 py-4">Người dùng</th><th className="px-5 py-4">Vai trò</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4">Đăng nhập gần nhất</th><th className="px-5 py-4">Thao tác</th></tr></thead><tbody>{users.map((user) => <tr className="border-t border-stone-100 transition hover:bg-rose-50/30" key={user.id}><td className="px-5 py-4"><div className="flex min-w-56 items-center gap-3"><span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-stone-900 font-black text-white">{(user.fullName || user.email).charAt(0).toUpperCase()}</span><span><strong className="block text-stone-900">{user.fullName}</strong><span className="text-stone-500">{user.email}</span></span></div></td><td className="px-5 py-4 font-semibold text-stone-700">{user.role === "ADMIN" ? "Quản trị viên" : "Khách hàng"}</td><td className="px-5 py-4"><StatusBadge label={user.status === "ACTIVE" ? "Đang hoạt động" : user.status === "LOCKED" ? "Đã khóa" : "Vô hiệu hóa"} tone={user.status === "ACTIVE" ? "success" : "warning"} /></td><td className="px-5 py-4 text-stone-500">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString("vi-VN") : "Chưa đăng nhập"}</td><td className="px-5 py-4"><div className="flex min-w-72 flex-wrap gap-2"><Button variant="ghost" onClick={() => void showDetail(user.id)}>Chi tiết</Button><Button variant="outline" onClick={() => void manage(user, { role: user.role === "ADMIN" ? "CUSTOMER" : "ADMIN" }, "đổi vai trò")}>Đổi vai trò</Button>{user.status === "ACTIVE" ? <Button variant="outline" onClick={() => void manage(user, { status: "LOCKED" }, "khóa tài khoản")}>Khóa</Button> : <Button variant="outline" onClick={() => void manage(user, { status: "ACTIVE" }, "mở khóa tài khoản")}>Mở khóa</Button>}{user.status !== "DISABLED" ? <Button variant="destructive" onClick={() => void manage(user, { status: "DISABLED" }, "vô hiệu hóa tài khoản")}>Vô hiệu hóa</Button> : null}</div></td></tr>)}</tbody></table></div>}
    </SurfacePanel>
    {selected ? <SurfacePanel className="border-rose-200"><div className="flex flex-wrap items-start justify-between gap-3"><div className="flex items-center gap-3"><span className="grid h-12 w-12 place-items-center rounded-full bg-rose-100 text-lg font-black text-rose-700">{(selected.fullName || selected.email).charAt(0).toUpperCase()}</span><div><h2 className="text-lg font-black text-stone-900">{selected.fullName}</h2><p className="text-sm text-stone-500">{selected.email} · {selected.phone || "Chưa có số điện thoại"}</p><p className="mt-1 text-xs text-stone-500">Tạo lúc {new Date(selected.createdAt).toLocaleString("vi-VN")}</p></div></div><Button variant="ghost" onClick={() => setSelected(null)}>Đóng</Button></div><div className="mt-6 flex items-center gap-2 border-b border-stone-100 pb-3"><History className="h-5 w-5 text-rose-600" /><h3 className="font-bold">Lịch sử thao tác</h3></div>{selected.audits?.length ? <div className="mt-4 space-y-3">{selected.audits.map((audit) => <div className="rounded-2xl border border-stone-200 bg-stone-50 p-4 text-sm" key={audit.id}><div className="flex flex-wrap justify-between gap-2"><strong>{audit.action}</strong><span className="text-stone-500">{new Date(audit.createdAt).toLocaleString("vi-VN")}</span></div><p className="mt-2">{audit.reason}</p><p className="mt-2 text-xs text-stone-500">Vai trò: {audit.oldRole} → {audit.newRole}; trạng thái: {audit.oldStatus} → {audit.newStatus}</p></div>)}</div> : <p className="mt-4 text-sm text-stone-500">Chưa có lịch sử quản trị.</p>}</SurfacePanel> : null}
  </div>;
}

function Summary({ icon: Icon, label, value }: { icon: typeof UsersRound; label: string; value: number }) {
  return <div className="rounded-2xl border border-stone-200 bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><div><p className="text-sm font-semibold text-stone-500">{label}</p><p className="mt-1 text-3xl font-black text-stone-900">{value}</p></div><span className="grid h-11 w-11 place-items-center rounded-2xl bg-rose-50 text-rose-600"><Icon className="h-5 w-5" /></span></div></div>;
}
