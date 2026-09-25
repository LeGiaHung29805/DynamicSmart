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
  const [users, setUsers] = useState<AdminUser[]>([]); const [query, setQuery] = useState(""); const [status, setStatus] = useState(""); const [message, setMessage] = useState("Đang tải…");
  const load = () => customerApi.users(query, status).then((page) => { setUsers(page.content); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách người dùng."));
  useEffect(() => { const timer = setTimeout(() => { customerApi.users(query, status).then((page) => { setUsers(page.content); setMessage(""); }).catch(() => setMessage("Không thể tải danh sách người dùng.")); }, 250); return () => clearTimeout(timer); }, [query, status]);
  const changeStatus = async (user: AdminUser) => {
    const next = user.status === "ACTIVE" ? "LOCKED" : "ACTIVE"; const reason = window.prompt(`Lý do ${next === "LOCKED" ? "khóa" : "mở khóa"} tài khoản:`)?.trim();
    if (!reason) return;
    try { await customerApi.manageUser(user.id, { status: next, reason }); load(); } catch { setMessage("Không thể cập nhật tài khoản. Bạn không thể tự thay đổi chính mình hoặc vô hiệu hóa admin cuối cùng."); }
  };
  const changeRole = async (user: AdminUser) => {
    const next = user.role === "ADMIN" ? "CUSTOMER" : "ADMIN"; const reason = window.prompt(`Lý do đổi vai trò thành ${next}:`)?.trim();
    if (!reason) return;
    try { await customerApi.manageUser(user.id, { role: next, reason }); load(); } catch { setMessage("Không thể đổi vai trò tài khoản."); }
  };
  return <div className="space-y-7"><PageHeader eyebrow="Quản lý tài khoản" title="Người dùng" description="Mọi thay đổi trạng thái/vai trò đều gửi Idempotency-Key, lý do và được backend ghi audit." />
    <SurfacePanel><div className="grid gap-4 sm:grid-cols-[1fr_220px]"><Input label="Tìm kiếm" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên hoặc email" /><Select label="Trạng thái" value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="DISABLED">Vô hiệu hóa</option></Select></div>
      {message ? <p className={`mt-4 text-sm ${message.startsWith("Không") ? "text-red-600" : "text-slate-500"}`}>{message}</p> : null}
      <div className="mt-6 overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr><th className="px-4 py-3">Người dùng</th><th className="px-4 py-3">Vai trò</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3">Đăng nhập gần nhất</th><th className="px-4 py-3 text-right">Thao tác</th></tr></thead><tbody>{users.map((user) => <tr className="border-t" key={user.id}><td className="px-4 py-4"><strong className="block">{user.fullName}</strong><span className="text-slate-500">{user.email}</span></td><td className="px-4 py-4">{user.role}</td><td className="px-4 py-4"><StatusBadge label={user.status} tone={user.status === "ACTIVE" ? "success" : "warning"} /></td><td className="px-4 py-4 text-slate-500">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString("vi-VN") : "Chưa đăng nhập"}</td><td className="px-4 py-4"><div className="flex justify-end gap-2"><Button variant="outline" onClick={() => void changeRole(user)}>Đổi vai trò</Button><Button variant="outline" onClick={() => void changeStatus(user)}>{user.status === "ACTIVE" ? "Khóa" : "Mở khóa"}</Button></div></td></tr>)}</tbody></table></div>
    </SurfacePanel></div>;
}
