export function catalogStatusLabel(value: string) {
  const labels: Record<string, string> = {
    ACTIVE: "Đang hoạt động",
    INACTIVE: "Ngừng hoạt động",
    DRAFT: "Bản nháp",
    ARCHIVED: "Đã lưu trữ",
    OUT_OF_STOCK: "Hết hàng",
    IN_STOCK: "Còn hàng",
    LOW_STOCK: "Sắp hết hàng",
  };
  return labels[value] ?? value;
}

export function attributeDataTypeLabel(value: string) {
  return ({
    TEXT: "Văn bản",
    NUMBER: "Số nguyên",
    DECIMAL: "Số thập phân",
    BOOLEAN: "Đúng / sai",
    SELECT: "Chọn một",
    MULTI_SELECT: "Chọn nhiều",
  } as Record<string, string>)[value] ?? "Chưa xác định";
}
