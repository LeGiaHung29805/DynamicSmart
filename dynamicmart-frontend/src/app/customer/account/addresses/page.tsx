import type { Metadata } from "next";
import { AddressBookPage } from "@/features/customer";
export const metadata: Metadata = { title: "Sổ địa chỉ" };
export default function Page() { return <AddressBookPage />; }
