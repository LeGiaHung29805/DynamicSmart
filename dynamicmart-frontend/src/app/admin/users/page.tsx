import type { Metadata } from "next";
import { AdminSectionPage } from "@/features/admin";

export const metadata: Metadata = { title: "Quản lý người dùng" };

export default function AdminUsersPage() {
  return <AdminSectionPage section="users" />;
}
