export function catalogStatusLabel(value: string) {
  return ({
    ACTIVE: "Đang hoạt động",
    INACTIVE: "Ngừng hoạt động",
    DRAFT: "Bản nháp",
    ARCHIVED: "Đã lưu trữ",
  } as Record<string, string>)[value] ?? "Chưa xác định";
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
