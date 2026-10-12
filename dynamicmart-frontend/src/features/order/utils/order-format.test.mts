import test from "node:test";
import assert from "node:assert/strict";
import {
  orderStatusLabel,
  orderTimelineReasonLabel,
  orderCancelReasonLabel,
} from "./order-format.ts";
import {
  catalogStatusLabel,
  attributeDataTypeLabel,
} from "../../catalog/utils/catalog-format.ts";
import {
  paymentStatusLabel,
  paymentAttemptStatusLabel,
} from "../../payment/utils/payment-format.ts";

test("orderStatusLabel translates raw statuses into Vietnamese", () => {
  assert.equal(orderStatusLabel("ORDER_CREATED"), "Đã tạo đơn hàng");
  assert.equal(orderStatusLabel("PENDING_PAYMENT"), "Chờ thanh toán");
  assert.equal(orderStatusLabel("CONFIRMED"), "Đã xác nhận");
  assert.equal(orderStatusLabel("PACKING"), "Đang đóng gói");
  assert.equal(orderStatusLabel("SHIPPING"), "Đang vận chuyển");
  assert.equal(orderStatusLabel("DELIVERED"), "Đã giao hàng");
  assert.equal(orderStatusLabel("COMPLETED"), "Hoàn tất");
  assert.equal(orderStatusLabel("CANCELLED"), "Đã hủy");
  assert.equal(orderStatusLabel("PAID"), "Đã thanh toán");
});

test("orderTimelineReasonLabel translates raw history reasons into Vietnamese", () => {
  assert.equal(orderTimelineReasonLabel("ORDER_CREATED"), "Khởi tạo đơn hàng thành công");
  assert.equal(orderTimelineReasonLabel("ADMIN_PACK"), "Quản trị viên xác nhận bắt đầu đóng gói kiện hàng");
  assert.equal(orderTimelineReasonLabel("ADMIN_SHIP"), "Quản trị viên chuyển kiện hàng cho đơn vị vận chuyển");
  assert.equal(orderTimelineReasonLabel("ADMIN_HANDOVER"), "Quản trị viên xác nhận đã giao và chờ khách xác nhận nhận hàng");
  assert.equal(orderTimelineReasonLabel("CUSTOMER_CONFIRM_RECEIVED"), "Khách hàng xác nhận đã nhận hàng thành công");
  assert.equal(orderTimelineReasonLabel("PAYMENT_SUCCEEDED"), "Ghi nhận thanh toán thành công");
  assert.equal(orderTimelineReasonLabel("PAYMENT_FAILED"), "Thanh toán thất bại");
  assert.equal(orderTimelineReasonLabel("PAYMENT_EXPIRED"), "Hết hạn thời gian thanh toán");
  assert.equal(orderTimelineReasonLabel("PaymentSucceeded"), "Ghi nhận thanh toán thành công");
  assert.equal(orderTimelineReasonLabel("PaymentFailed"), "Thanh toán thất bại");
  assert.equal(orderTimelineReasonLabel("PaymentExpired"), "Hết hạn thời gian thanh toán");
  assert.equal(orderTimelineReasonLabel("UNKNOWN_EVENT"), "Cập nhật trạng thái đơn hàng");
});

test("orderCancelReasonLabel translates cancellation reasons", () => {
  assert.equal(orderCancelReasonLabel("PAYMENT_FAILED"), "Thanh toán không thành công");
  assert.equal(orderCancelReasonLabel("PAYMENT_EXPIRED"), "Quá thời hạn thanh toán");
  assert.equal(orderCancelReasonLabel("CUSTOMER_CANCELLED"), "Khách hàng yêu cầu hủy đơn");
  assert.equal(orderCancelReasonLabel("OUT_OF_STOCK"), "Sản phẩm đã hết hàng");
  assert.equal(orderCancelReasonLabel("UNKNOWN_REASON"), "Đơn hàng được hủy theo quy trình xử lý");
});

test("catalogStatusLabel & attributeDataTypeLabel translate correctly", () => {
  assert.equal(catalogStatusLabel("ACTIVE"), "Đang hoạt động");
  assert.equal(catalogStatusLabel("INACTIVE"), "Ngừng hoạt động");
  assert.equal(catalogStatusLabel("DRAFT"), "Bản nháp");
  assert.equal(catalogStatusLabel("ARCHIVED"), "Đã lưu trữ");
  assert.equal(catalogStatusLabel("UNKNOWN_STATUS"), "Chưa xác định");
  assert.equal(attributeDataTypeLabel("TEXT"), "Văn bản");
  assert.equal(attributeDataTypeLabel("SELECT"), "Chọn một");
});

test("payment format labels translate correctly", () => {
  assert.equal(paymentStatusLabel("PENDING"), "Chờ thanh toán");
  assert.equal(paymentStatusLabel("PAID"), "Đã thanh toán");
  assert.equal(paymentAttemptStatusLabel("SUCCEEDED"), "Thành công");
  assert.equal(paymentAttemptStatusLabel("FAILED"), "Thất bại");
  assert.equal(paymentAttemptStatusLabel("EXPIRED"), "Hết hạn");
});
