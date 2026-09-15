import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: {
    default: "DynamicMart",
    template: "%s | DynamicMart",
  },
  description: "Nền tảng thương mại điện tử B2C DynamicMart.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi" className="font-sans">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
