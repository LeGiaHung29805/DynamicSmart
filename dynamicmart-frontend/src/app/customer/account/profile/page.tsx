import type { Metadata } from "next";
import { ProfilePage } from "@/features/customer";
export const metadata: Metadata = { title: "Hồ sơ cá nhân" };
export default function Page() { return <ProfilePage />; }
