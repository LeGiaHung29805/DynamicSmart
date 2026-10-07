export const mockReviews = [
  { id: "rv-1", customer: "Nguyen Minh", product: "Ao polo DynamicDry", rating: 5, status: "VISIBLE", content: "Vai mem, dung size, giao hang nhanh.", createdAt: "2026-10-02T09:15:00+07:00" },
  { id: "rv-2", customer: "Tran Ha", product: "Giay Urban Runner", rating: 4, status: "VISIBLE", content: "De di ca ngay, hop voi di lam.", createdAt: "2026-10-03T14:30:00+07:00" },
  { id: "rv-3", customer: "Le Quan", product: "Balo City Flex", rating: 2, status: "HIDDEN", content: "Noi dung da bi an.", createdAt: "2026-10-04T18:12:00+07:00" },
];

export const mockQuestions = [
  { id: "q-1", product: "Ao polo DynamicDry", customer: "Nguyen Minh", status: "ANSWERED", question: "Ao co bi xù sau khi giat may khong?", answer: "Nen giat mat trai va dung tui giat, vai giu form tot." },
  { id: "q-2", product: "Giay Urban Runner", customer: "Tran Ha", status: "OPEN", question: "Size 39 bao gio co lai?", answer: "" },
  { id: "q-3", product: "Balo City Flex", customer: "Le Quan", status: "HIDDEN", question: "Noi dung vi pham.", answer: "" },
];

export const mockWishlist = [
  { id: "wl-1", name: "Ao polo DynamicDry", price: 349000, savedAt: "2026-10-01T08:20:00+07:00" },
  { id: "wl-2", name: "Balo City Flex", price: 729000, savedAt: "2026-10-04T20:10:00+07:00" },
  { id: "wl-3", name: "Giay Urban Runner", price: 1190000, savedAt: "2026-10-05T11:40:00+07:00" },
];

export const mockNotifications = [
  { id: "nt-1", type: "ORDER_COMPLETED", title: "Don hang da hoan tat", content: "Don #DM24091 da du dieu kien danh gia.", read: false, createdAt: "2026-10-05T10:00:00+07:00" },
  { id: "nt-2", type: "PAYMENT_SUCCEEDED", title: "Thanh toan thanh cong", content: "He thong da ghi nhan thanh toan VNPay.", read: true, createdAt: "2026-10-04T13:00:00+07:00" },
  { id: "nt-3", type: "QUESTION_ANSWERED", title: "Cau hoi da duoc tra loi", content: "Admin da phan hoi cau hoi ve Ao polo DynamicDry.", read: false, createdAt: "2026-10-03T16:45:00+07:00" },
];

export const mockConversations = [
  { id: "cv-1", customer: "Nguyen Minh", status: "OPEN", assignedAdmin: "Admin Linh", lastMessageAt: "2026-10-05T15:12:00+07:00", messages: [
    { senderRole: "CUSTOMER", content: "Minh muon doi size ao polo.", createdAt: "15:02" },
    { senderRole: "ADMIN", content: "Minh gui ma don hang giup shop nhe.", createdAt: "15:12" },
  ] },
  { id: "cv-2", customer: "Tran Ha", status: "CLOSED", assignedAdmin: "Admin Nam", lastMessageAt: "2026-10-04T09:30:00+07:00", messages: [
    { senderRole: "CUSTOMER", content: "Minh can hoa don.", createdAt: "09:12" },
    { senderRole: "ADMIN", content: "Hoa don da duoc gui vao email tai khoan.", createdAt: "09:30" },
  ] },
];

export const mockSalesMetrics = [
  { date: "2026-10-01", revenue: 18500000, orders: 42 },
  { date: "2026-10-02", revenue: 21300000, orders: 49 },
  { date: "2026-10-03", revenue: 19800000, orders: 44 },
  { date: "2026-10-04", revenue: 24750000, orders: 57 },
  { date: "2026-10-05", revenue: 23200000, orders: 53 },
];

export const mockBestSellers = [
  { product: "Ao polo DynamicDry", variant: "Navy / M", quantity: 128, revenue: 44672000 },
  { product: "Giay Urban Runner", variant: "Black / 40", quantity: 64, revenue: 76160000 },
  { product: "Balo City Flex", variant: "Graphite", quantity: 51, revenue: 37179000 },
];
