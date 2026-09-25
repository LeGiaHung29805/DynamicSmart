import type { Metadata } from "next";
import { AdminUsersPage as AdminUsersFeature } from "@/features/customer";

export const metadata: Metadata = { title: "Quản lý người dùng" };

export default function AdminUsersPage() {
  return <AdminUsersFeature />;
}
