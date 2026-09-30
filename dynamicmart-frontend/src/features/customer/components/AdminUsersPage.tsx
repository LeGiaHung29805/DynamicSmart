"use client";

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

  return <div className="space-y-7"><PageHeader eyebrow="Quản lý tài khoản" title="Người dùng" description="Mọi thay đổi trạng thái/vai trò đều có Idempotency-Key, lý do và audit bất biến." />
    <SurfacePanel><div className="grid gap-4 md:grid-cols-[1fr_180px_180px]"><Input label="Tìm kiếm" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc email" /><Select label="Vai trò" value={role} onChange={(event) => setRole(event.target.value)}><option value="">Tất cả</option><option value="CUSTOMER">Khách hàng</option><option value="ADMIN">Quản trị viên</option></Select><Select label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="DISABLED">Vô hiệu hóa</option></Select></div>
      {message ? <p className={`mt-4 text-sm ${message.startsWith("Không") ? "text-red-600" : "text-slate-500"}`}>{message}</p> : null}
      <div className="mt-6 overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Người dùng</th><th className="px-4 py-3">Vai trò</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3">Đăng nhập gần nhất</th><th className="px-4 py-3">Thao tác</th></tr></thead><tbody>{users.map((user) => <tr className="border-t" key={user.id}><td className="px-4 py-4"><strong className="block">{user.fullName}</strong><span className="text-slate-500">{user.email}</span></td><td className="px-4 py-4">{user.role}</td><td className="px-4 py-4"><StatusBadge label={user.status} tone={user.status === "ACTIVE" ? "success" : "warning"} /></td><td className="px-4 py-4 text-slate-500">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString("vi-VN") : "Chưa đăng nhập"}</td><td className="px-4 py-4"><div className="flex flex-wrap gap-2"><Button variant="ghost" onClick={() => void showDetail(user.id)}>Chi tiết</Button><Button variant="outline" onClick={() => void manage(user, { role: user.role === "ADMIN" ? "CUSTOMER" : "ADMIN" }, "đổi vai trò")}>Đổi vai trò</Button>{user.status === "ACTIVE" ? <Button variant="outline" onClick={() => void manage(user, { status: "LOCKED" }, "khóa tài khoản")}>Khóa</Button> : <Button variant="outline" onClick={() => void manage(user, { status: "ACTIVE" }, "mở khóa tài khoản")}>Mở khóa</Button>}{user.status !== "DISABLED" ? <Button variant="destructive" onClick={() => void manage(user, { status: "DISABLED" }, "vô hiệu hóa tài khoản")}>Vô hiệu hóa</Button> : null}</div></td></tr>)}</tbody></table></div>
    </SurfacePanel>
    {selected ? <SurfacePanel><div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-lg font-black">{selected.fullName}</h2><p className="text-sm text-slate-500">{selected.email} · {selected.phone || "Chưa có số điện thoại"}</p><p className="mt-1 text-xs text-slate-500">Tạo lúc {new Date(selected.createdAt).toLocaleString("vi-VN")}</p></div><Button variant="ghost" onClick={() => setSelected(null)}>Đóng</Button></div><h3 className="mt-6 font-bold">Lịch sử thao tác</h3>{selected.audits?.length ? <div className="mt-3 space-y-2">{selected.audits.map((audit) => <div className="rounded-xl border p-3 text-sm" key={audit.id}><strong>{audit.action}</strong><span className="ml-2 text-slate-500">{new Date(audit.createdAt).toLocaleString("vi-VN")}</span><p className="mt-1">{audit.reason}</p><p className="mt-1 text-xs text-slate-500">Role: {audit.oldRole} → {audit.newRole}; Status: {audit.oldStatus} → {audit.newStatus}</p></div>)}</div> : <p className="mt-3 text-sm text-slate-500">Chưa có lịch sử quản trị.</p>}</SurfacePanel> : null}
  </div>;
}
