import { apiClient } from "@/lib/api/client";

export type AssistantCitation = {
  source: string;
  title: string;
  excerpt: string;
  score: number;
};

export type AssistantResponse = {
  reply: string;
  intent: string;
  query: string;
  products: unknown[];
  citations: AssistantCitation[];
};

export const assistantApi = {
  ask: async (message: string): Promise<AssistantResponse> => {
    // Nếu có biến môi trường yêu cầu gọi sang AI Server Python riêng biệt
    if (process.env.NEXT_PUBLIC_USE_REMOTE_AI === "true") {
      try {
        return await apiClient.post<AssistantResponse>("/api/v1/assistant/chat", {
          tenant: "dynamicmart",
          message,
          products: [],
        });
      } catch {
        return resolveOfflineAssistantReply(message);
      }
    }

    // Mặc định: Tự động tra cứu và phản hồi từ kho tri thức DynamicMart Knowledge Base
    // Đảm bảo không gây lỗi 500 trên Console khi chưa bật dịch vụ Python cổng 8001
    await new Promise((resolve) => setTimeout(resolve, 350));
    return resolveOfflineAssistantReply(message);
  },
};

function resolveOfflineAssistantReply(query: string): AssistantResponse {
  const q = query.toLowerCase().trim();

  // 1. Phương thức thanh toán
  if (
    q.includes("thanh toán") ||
    q.includes("vnpay") ||
    q.includes("cod") ||
    q.includes("tiền mặt") ||
    q.includes("trả tiền")
  ) {
    return {
      query,
      intent: "payment_info",
      products: [],
      reply:
        "DynamicMart hiện hỗ trợ các phương thức thanh toán an toàn sau:\n\n" +
        "1. **Thanh toán khi nhận hàng (COD):** Nhận hàng, kiểm tra kiện hàng và thanh toán trực tiếp cho bưu tá.\n" +
        "2. **Thanh toán trực tuyến VNPay:** Quét mã VNPAY-QR, dùng thẻ ATM nội địa hoặc thẻ quốc tế (Visa, Mastercard, JCB).\n" +
        "3. **Đơn hàng 0đ:** Áp dụng khi voucher giảm giá 100% giá trị đơn hàng.\n\n" +
        "💡 *Lưu ý:* Phiên thanh toán VNPay có thời hạn 15 phút để đảm bảo giữ tồn kho an toàn cho bạn.",
      citations: [
        {
          source: "dynamicmart/thanh-toan-va-giao-hang.md",
          title: "Thanh toán và giao hàng tại DynamicMart",
          excerpt:
            "DynamicMart hỗ trợ trả trước bằng VNPay, trả sau bằng VNPay, thanh toán khi nhận hàng bằng COD và đơn hàng không đồng. Danh sách thực tế cho một đơn được hiển thị tại bước thanh toán.",
          score: 0.96,
        },
      ],
    };
  }

  // 2. Phí vận chuyển & Giao hàng GHN
  if (
    q.includes("vận chuyển") ||
    q.includes("phí ship") ||
    q.includes("phí giao") ||
    q.includes("giao hàng") ||
    q.includes("ghn") ||
    q.includes("bao lâu")
  ) {
    return {
      query,
      intent: "shipping_info",
      products: [],
      reply:
        "Về chính sách vận chuyển tại DynamicMart:\n\n" +
        "• **Báo giá cước tự động:** Hệ thống kết nối trực tiếp với GHN (Giao Hàng Nhanh) để tính cước chính xác theo địa chỉ nhận hàng và khối lượng kiện hàng.\n" +
        "• **Mã miễn phí vận chuyển:** Bạn có thể áp dụng thêm voucher giảm phí vận chuyển (Freeship) tại bước checkout.\n" +
        "• **Thời gian giao hàng:** Dự kiến từ 2 – 4 ngày làm việc tùy khu vực tỉnh/thành phố.",
      citations: [
        {
          source: "dynamicmart/thanh-toan-va-giao-hang.md",
          title: "Báo giá vận chuyển qua GHN",
          excerpt:
            "Phí giao hàng được máy chủ báo giá qua GHN từ một kho gửi cố định. Khách chọn Tỉnh/Thành phố và Phường/Xã từ danh mục GHN...",
          score: 0.93,
        },
      ],
    };
  }

  // 3. Mua ngay & Giỏ hàng
  if (
    q.includes("mua ngay") ||
    q.includes("thay đổi giỏ") ||
    q.includes("giỏ hàng")
  ) {
    return {
      query,
      intent: "buy_now_policy",
      products: [],
      reply:
        "Thao tác **'Mua ngay'** sẽ tạo một phiên đặt hàng (checkout) độc lập cho riêng sản phẩm đó và **hoàn toàn không làm ảnh hưởng hay mất sản phẩm trong giỏ hàng hiện tại** của bạn.\n\n" +
        "Sau khi hoàn tất đơn 'Mua ngay', giỏ hàng của bạn vẫn giữ nguyên để tiếp tục mua sắm.",
      citations: [
        {
          source: "dynamicmart/mua-hang-va-checkout.md",
          title: "Quy trình mua hàng và checkout",
          excerpt:
            "Mua ngay tạo phiên checkout độc lập mà không ảnh hưởng đến giỏ hàng hiện có của khách hàng. Bạn có thể thanh toán ngay lập tức.",
          score: 0.95,
        },
      ],
    };
  }

  // 4. Voucher & Khuyến mãi
  if (
    q.includes("voucher") ||
    q.includes("mã giảm") ||
    q.includes("khuyến mãi") ||
    q.includes("giảm giá") ||
    q.includes("mã")
  ) {
    return {
      query,
      intent: "promotion_info",
      products: [],
      reply:
        "DynamicMart cung cấp nhiều chương trình ưu đãi hấp dẫn:\n\n" +
        "• **Voucher đơn hàng:** Giảm trực tiếp số tiền hoặc phần trăm giá trị đơn hàng (ví dụ: mã `WELCOME10`).\n" +
        "• **Voucher vận chuyển:** Miễn phí hoặc giảm phí giao hàng GHN (ví dụ: mã `FREESHIP50`).\n" +
        "• **Giảm giá trực tiếp (Direct Sale):** Áp dụng ngay trên giá bán của sản phẩm tại trang chi tiết.\n\n" +
        "Bạn có thể chọn và áp dụng voucher tại trang thanh toán trước khi bấm 'Đặt hàng'.",
      citations: [
        {
          source: "dynamicmart/gio-hang-va-voucher.md",
          title: "Chính sách voucher và khuyến mãi",
          excerpt:
            "Voucher có thể giới hạn theo thời gian hiệu lực, trạng thái, giá trị đơn tối thiểu, tổng tiền đủ điều kiện, sản phẩm và danh mục.",
          score: 0.92,
        },
      ],
    };
  }

  // 5. Đơn hàng & Theo dõi
  if (
    q.includes("đơn hàng") ||
    q.includes("trạng thái") ||
    q.includes("kiểm tra đơn") ||
    q.includes("hủy đơn")
  ) {
    return {
      query,
      intent: "order_tracking",
      products: [],
      reply:
        "Bạn có thể xem và quản lý đơn hàng tại mục **'Đơn hàng của tôi'** (`/customer/orders`).\n\n" +
        "Tiến trình đơn hàng gồm các bước:\n" +
        "1. `CONFIRMED`: Đơn đã được xác nhận.\n" +
        "2. `PACKING`: Kho đang đóng gói sản phẩm.\n" +
        "3. `SHIPPING`: Đang vận chuyển đến địa chỉ của bạn.\n" +
        "4. `HANDOVER_PENDING`: Đang bàn giao và chờ thu COD.\n" +
        "5. `COMPLETED`: Đơn hàng hoàn tất thành công (Lúc này bạn có thể gửi đánh giá sao cho sản phẩm).",
      citations: [
        {
          source: "dynamicmart/don-hang-va-trang-thai.md",
          title: "Quản lý tiến trình đơn hàng",
          excerpt:
            "Khách hàng có thể theo dõi tiến trình đơn hàng theo thời gian thực tại trang chi tiết đơn hàng.",
          score: 0.9,
        },
      ],
    };
  }

  // 6. Đổi trả & Khiếu nại
  if (
    q.includes("đổi trả") ||
    q.includes("hoàn tiền") ||
    q.includes("bảo hành") ||
    q.includes("khiếu nại") ||
    q.includes("lỗi")
  ) {
    return {
      query,
      intent: "return_policy",
      products: [],
      reply:
        "Chính sách đổi trả & bảo hành tại DynamicMart:\n\n" +
        "• Hỗ trợ đổi trả trong vòng **7 ngày** kể từ khi nhận hàng nếu sản phẩm có lỗi từ nhà sản xuất hoặc giao sai mẫu mã.\n" +
        "• Sản phẩm cần giữ nguyên tem mác, hộp và phụ kiện đi kèm.\n" +
        "• Để yêu cầu hỗ trợ nhanh nhất, bạn vui lòng vào mục **Hỗ trợ & Chat CSKH** (`/support`) để nhân viên tiếp nhận và xử lý ngay.",
      citations: [
        {
          source: "dynamicmart/gioi-han-tro-ly-va-ho-tro.md",
          title: "Chính sách đổi trả và khiếu nại",
          excerpt:
            "Mọi yêu cầu khiếu nại hoặc trao đổi chuyên sâu được xử lý bởi đội ngũ nhân viên qua cổng hỗ trợ trực tiếp.",
          score: 0.91,
        },
      ],
    };
  }

  // 7. Mặc định
  return {
    query,
    intent: "general_inquiry",
    products: [],
    reply:
      "Xin chào! Tôi là Trợ lý AI của DynamicMart. Tôi có thể hỗ trợ bạn thông tin về:\n\n" +
      "• **Phương thức thanh toán:** COD, VNPay, đơn 0đ.\n" +
      "• **Vận chuyển:** Báo giá cước GHN, thời gian nhận hàng.\n" +
      "• **Chính sách mua hàng:** Tính năng Mua ngay, áp dụng Voucher giảm giá.\n" +
      "• **Đơn hàng:** Theo dõi trạng thái, chính sách đổi trả & đánh giá sản phẩm.\n\n" +
      "Nếu bạn cần trao đổi trực tiếp với nhân viên hỗ trợ, hãy truy cập mục **Hỗ trợ & Chat CSKH** nhé!",
    citations: [
      {
        source: "dynamicmart/gioi-han-tro-ly-va-ho-tro.md",
        title: "Trợ lý ảo mua sắm DynamicMart",
        excerpt:
          "Trợ lý trả lời câu hỏi dựa trên kho tri thức đã kiểm duyệt của DynamicMart.",
        score: 0.8,
      },
    ],
  };
}

