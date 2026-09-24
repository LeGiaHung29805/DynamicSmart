"use client";

import { useEffect } from "react";
import { setAnonymousSession } from "@/lib/auth/session";
import { refreshSession } from "../api/auth.api";

/** Khôi phục access token trong bộ nhớ từ refresh cookie sau khi người dùng tải lại trang. */
export function SessionBootstrap() {
  useEffect(() => {
    void refreshSession().catch(() => {
      // Khách chưa đăng nhập là trạng thái bình thường, không hiện lỗi ở mọi trang.
      setAnonymousSession();
    });
  }, []);

  return null;
}
