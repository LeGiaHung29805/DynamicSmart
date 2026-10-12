package com.dynamicmart.api_gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    public record AssistantCitation(String source, String title, String excerpt, double score) {}
    public record AssistantResponse(String reply, String intent, String query, List<Object> products, List<AssistantCitation> citations) {}

    private final String assistantServiceUrl;
    private final RestClient restClient;

    public AssistantController(@Value("${app.routes.ai-assistant-service-url}") String assistantServiceUrl) {
        this.assistantServiceUrl = assistantServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(assistantServiceUrl)
                .build();
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        String message = request.getOrDefault("message", "").toString();
        try {
            // Cố gắng gọi sang AI service thật nếu đang bật ở cổng 8001
            Object response = restClient.post()
                    .uri("/api/v1/assistant/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Object.class);
            if (response != null) {
                return ResponseEntity.ok(response);
            }
        } catch (Exception ignored) {
            // Khi ai-assistant-service ở cổng 8001 chưa bật -> fallback về kho tri thức kiến thức DynamicMart
        }
        return ResponseEntity.ok(resolveFallback(message));
    }

    private AssistantResponse resolveFallback(String query) {
        String q = query.toLowerCase().trim();
        if (q.contains("thanh toán") || q.contains("vnpay") || q.contains("cod") || q.contains("tiền mặt") || q.contains("trả tiền")) {
            return new AssistantResponse(
                    "DynamicMart hiện hỗ trợ các phương thức thanh toán an toàn sau:\n\n" +
                    "1. **Thanh toán khi nhận hàng (COD):** Nhận hàng, kiểm tra kiện hàng và thanh toán trực tiếp cho bưu tá.\n" +
                    "2. **Thanh toán trực tuyến VNPay:** Quét mã VNPAY-QR, dùng thẻ ATM nội địa hoặc thẻ quốc tế (Visa, Mastercard, JCB).\n" +
                    "3. **Đơn hàng 0đ:** Áp dụng khi voucher giảm giá 100% giá trị đơn hàng.\n\n" +
                    "💡 *Lưu ý:* Phiên thanh toán VNPay có thời hạn 15 phút để đảm bảo giữ tồn kho an toàn cho bạn.",
                    "payment_info",
                    query,
                    List.of(),
                    List.of(new AssistantCitation(
                            "dynamicmart/thanh-toan-va-giao-hang.md",
                            "Thanh toán và giao hàng tại DynamicMart",
                            "DynamicMart hỗ trợ trả trước bằng VNPay, trả sau bằng VNPay, thanh toán khi nhận hàng bằng COD và đơn hàng không đồng. Danh sách thực tế cho một đơn được hiển thị tại bước thanh toán.",
                            0.96
                    ))
            );
        }
        if (q.contains("vận chuyển") || q.contains("phí ship") || q.contains("phí giao") || q.contains("giao hàng") || q.contains("ghn") || q.contains("bao lâu")) {
            return new AssistantResponse(
                    "Về chính sách vận chuyển tại DynamicMart:\n\n" +
                    "• **Báo giá cước tự động:** Hệ thống kết nối trực tiếp với GHN (Giao Hàng Nhanh) để tính cước chính xác theo địa chỉ nhận hàng và khối lượng kiện hàng.\n" +
                    "• **Mã miễn phí vận chuyển:** Bạn có thể áp dụng thêm voucher giảm phí vận chuyển (Freeship) tại bước checkout.\n" +
                    "• **Thời gian giao hàng:** Dự kiến từ 2 – 4 ngày làm việc tùy khu vực tỉnh/thành phố.",
                    "shipping_info",
                    query,
                    List.of(),
                    List.of(new AssistantCitation(
                            "dynamicmart/thanh-toan-va-giao-hang.md",
                            "Báo giá vận chuyển qua GHN",
                            "Phí giao hàng được máy chủ báo giá qua GHN từ một kho gửi cố định. Khách chọn Tỉnh/Thành phố và Phường/Xã từ danh mục GHN...",
                            0.93
                    ))
            );
        }
        if (q.contains("mua ngay") || q.contains("thay đổi giỏ") || q.contains("giỏ hàng")) {
            return new AssistantResponse(
                    "Thao tác **'Mua ngay'** sẽ tạo một phiên đặt hàng (checkout) độc lập cho riêng sản phẩm đó và **hoàn toàn không làm ảnh hưởng hay mất sản phẩm trong giỏ hàng hiện tại** của bạn.\n\n" +
                    "Sau khi hoàn tất đơn 'Mua ngay', giỏ hàng của bạn vẫn giữ nguyên để tiếp tục mua sắm.",
                    "buy_now_policy",
                    query,
                    List.of(),
                    List.of(new AssistantCitation(
                            "dynamicmart/mua-hang-va-checkout.md",
                            "Quy trình mua hàng và checkout",
                            "Mua ngay tạo phiên checkout độc lập mà không ảnh hưởng đến giỏ hàng hiện có của khách hàng. Bạn có thể thanh toán ngay lập tức.",
                            0.95
                    ))
            );
        }
        if (q.contains("voucher") || q.contains("mã giảm") || q.contains("khuyến mãi") || q.contains("giảm giá") || q.contains("mã")) {
            return new AssistantResponse(
                    "DynamicMart cung cấp nhiều chương trình ưu đãi hấp dẫn:\n\n" +
                    "• **Voucher đơn hàng:** Giảm trực tiếp số tiền hoặc phần trăm giá trị đơn hàng (ví dụ: mã `WELCOME10`).\n" +
                    "• **Voucher vận chuyển:** Miễn phí hoặc giảm phí giao hàng GHN (ví dụ: mã `FREESHIP50`).\n" +
                    "• **Giảm giá trực tiếp (Direct Sale):** Áp dụng ngay trên giá bán của sản phẩm tại trang chi tiết.\n\n" +
                    "Bạn có thể chọn và áp dụng voucher tại trang thanh toán trước khi bấm 'Đặt hàng'.",
                    "promotion_info",
                    query,
                    List.of(),
                    List.of(new AssistantCitation(
                            "dynamicmart/gio-hang-va-voucher.md",
                            "Chính sách voucher và khuyến mãi",
                            "Voucher có thể giới hạn theo thời gian hiệu lực, trạng thái, giá trị đơn tối thiểu, tổng tiền đủ điều kiện...",
                            0.92
                    ))
            );
        }
        if (q.contains("đơn hàng") || q.contains("trạng thái") || q.contains("kiểm tra đơn") || q.contains("hủy đơn")) {
            return new AssistantResponse(
                    "Bạn có thể xem và quản lý đơn hàng tại mục **'Đơn hàng của tôi'** (`/customer/orders`).\n\n" +
                    "Tiến trình đơn hàng gồm các bước:\n" +
                    "1. `CONFIRMED`: Đơn đã được xác nhận.\n" +
                    "2. `PACKING`: Kho đang đóng gói sản phẩm.\n" +
                    "3. `SHIPPING`: Đang vận chuyển đến địa chỉ của bạn.\n" +
                    "4. `HANDOVER_PENDING`: Đang bàn giao và chờ thu COD.\n" +
                    "5. `COMPLETED`: Đơn hàng hoàn tất thành công (Lúc này bạn có thể gửi đánh giá sao cho sản phẩm).",
                    "order_tracking",
                    query,
                    List.of(),
                    List.of(new AssistantCitation(
                            "dynamicmart/don-hang-va-trang-thai.md",
                            "Quản lý tiến trình đơn hàng",
                            "Khách hàng có thể theo dõi tiến trình đơn hàng theo thời gian thực tại trang chi tiết đơn hàng.",
                            0.9
                    ))
            );
        }
        return new AssistantResponse(
                "Xin chào! Tôi là Trợ lý AI của DynamicMart. Tôi có thể hỗ trợ bạn thông tin về:\n\n" +
                "• **Phương thức thanh toán:** COD, VNPay, đơn 0đ.\n" +
                "• **Vận chuyển:** Báo giá cước GHN, thời gian nhận hàng.\n" +
                "• **Chính sách mua hàng:** Tính năng Mua ngay, áp dụng Voucher giảm giá.\n" +
                "• **Đơn hàng:** Theo dõi trạng thái, chính sách đổi trả & đánh giá sản phẩm.\n\n" +
                "Nếu bạn cần trao đổi trực tiếp với nhân viên hỗ trợ, hãy truy cập mục **Hỗ trợ & Chat CSKH** nhé!",
                "general_inquiry",
                query,
                List.of(),
                List.of(new AssistantCitation(
                        "dynamicmart/gioi-han-tro-ly-va-ho-tro.md",
                        "Trợ lý ảo mua sắm DynamicMart",
                        "Trợ lý trả lời câu hỏi dựa trên kho tri thức đã kiểm duyệt của DynamicMart.",
                        0.8
                ))
        );
    }
}
