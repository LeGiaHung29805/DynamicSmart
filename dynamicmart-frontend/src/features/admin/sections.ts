export const adminSections = {
  users: {
    title: "Người dùng",
    eyebrow: "Quản lý tài khoản",
    description: "Xem trạng thái, vai trò và lịch sử tài khoản khi Identity API dành cho quản trị được kết nối.",
    columns: ["Người dùng", "Email", "Vai trò", "Trạng thái", "Ngày tạo"],
  },
  catalog: {
    title: "Danh mục & sản phẩm",
    eyebrow: "Quản lý catalog",
    description: "Một kiểu bảng và bộ lọc chung cho danh mục, sản phẩm và tồn kho trong các bước tiếp theo.",
    columns: ["Sản phẩm / danh mục", "Mã", "Giá", "Tồn kho", "Trạng thái"],
  },
  orders: {
    title: "Đơn hàng",
    eyebrow: "Vận hành bán hàng",
    description: "Theo dõi đơn hàng, thanh toán và trạng thái giao hàng khi Order API được kết nối.",
    columns: ["Mã đơn", "Khách hàng", "Tổng tiền", "Ngày đặt", "Trạng thái"],
  },
  reports: {
    title: "Báo cáo",
    eyebrow: "Phân tích kinh doanh",
    description: "Khung chung cho báo cáo doanh thu, sản phẩm và khách hàng khi Reporting API sẵn sàng.",
    columns: ["Báo cáo", "Kỳ dữ liệu", "Chỉ số chính", "Cập nhật", "Trạng thái"],
  },
} as const;

export type AdminSection = keyof typeof adminSections;
