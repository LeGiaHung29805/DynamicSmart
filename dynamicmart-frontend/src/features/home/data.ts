export const categories = [
  { icon: "cpu", name: "Điện tử & Công nghệ", caption: "Điện thoại · Laptop · Âm thanh", href: "/products?category=dien-tu", color: "from-cyan-100 to-sky-50 text-cyan-700" },
  { icon: "shirt", name: "Thời trang", caption: "Áo thun và phụ kiện", href: "/products?category=thoi-trang", color: "from-sky-100 to-blue-50 text-sky-700" },
  { icon: "house", name: "Nhà cửa & Đời sống", caption: "Đèn và đồ dùng tiện ích", href: "/products?category=nha-cua-doi-song", color: "from-lime-100 to-green-50 text-lime-700" },
  { icon: "sparkles", name: "Làm đẹp & Chăm sóc cá nhân", caption: "Chăm sóc da và cơ thể", href: "/products?category=lam-dep-cham-soc-ca-nhan", color: "from-rose-100 to-pink-50 text-rose-700" },
  { icon: "dumbbell", name: "Thể thao & Dã ngoại", caption: "Luyện tập và ngoài trời", href: "/products?category=the-thao-da-ngoai", color: "from-indigo-100 to-blue-50 text-indigo-700" },
  { icon: "bookOpen", name: "Sách & Văn phòng phẩm", caption: "Sổ và dụng cụ học tập", href: "/products?category=sach-van-phong-pham", color: "from-amber-100 to-orange-50 text-amber-700" },
] as const;

export const brands = [
  { name: "NIKE", subtitle: "Just do it", color: "from-zinc-950 to-zinc-800 text-white", mark: "N" },
  { name: "adidas", subtitle: "Impossible is nothing", color: "from-white to-zinc-100 text-zinc-950", mark: "A" },
  { name: "CASIO", subtitle: "Time for everyone", color: "from-blue-950 to-blue-800 text-white", mark: "C" },
  { name: "SEIKO", subtitle: "Since 1881", color: "from-emerald-950 to-emerald-800 text-white", mark: "S" },
  { name: "LEVI'S", subtitle: "Live in Levi's", color: "from-red-700 to-rose-600 text-white", mark: "L" },
  { name: "PUMA", subtitle: "Forever faster", color: "from-amber-300 to-yellow-200 text-zinc-950", mark: "P" },
  { name: "UNIQLO", subtitle: "LifeWear", color: "from-red-600 to-red-500 text-white", mark: "U" },
  { name: "SAMSUNG", subtitle: "Do what you can't", color: "from-indigo-950 to-blue-800 text-white", mark: "S" },
  { name: "SONY", subtitle: "Make believe", color: "from-slate-900 to-slate-700 text-white", mark: "S" },
  { name: "ZARA", subtitle: "New season", color: "from-stone-100 to-white text-stone-950", mark: "Z" },
] as const;

export const heroSlides = [
  {
    id: "dynamic-marketplace",
    eyebrow: "DynamicMart · Mua sắm đa ngành",
    title: "Mọi nhu cầu, trong một trải nghiệm.",
    description: "Khám phá sản phẩm công nghệ, thời trang, nhà cửa, làm đẹp, thể thao và văn phòng phẩm tại một nơi.",
    primaryLabel: "Khám phá sản phẩm",
    primaryHref: "/products",
    secondaryLabel: "Xem danh mục",
    secondaryHref: "#categories",
    image: "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1800&q=88",
    accent: "text-emerald-300",
  },
  {
    id: "technology",
    eyebrow: "Công nghệ cho cuộc sống",
    title: "Kết nối nhanh hơn. Tận hưởng nhiều hơn.",
    description: "Điện thoại, laptop và thiết bị âm thanh phù hợp cho học tập, công việc lẫn giải trí.",
    primaryLabel: "Xem đồ công nghệ",
    primaryHref: "/products?category=dien-tu",
    secondaryLabel: "Xem sản phẩm mới",
    secondaryHref: "/products?sort=newest",
    image: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1800&q=88",
    accent: "text-cyan-300",
  },
  {
    id: "home-living",
    eyebrow: "Nhà cửa & Đời sống",
    title: "Tiện nghi bắt đầu từ những điều nhỏ.",
    description: "Lựa chọn sản phẩm thiết thực để làm mới không gian và chăm sóc nhịp sống mỗi ngày.",
    primaryLabel: "Khám phá nhà cửa",
    primaryHref: "/products?category=nha-cua-doi-song",
    secondaryLabel: "Ưu đãi hôm nay",
    secondaryHref: "/products?promotion=true",
    image: "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1800&q=88",
    accent: "text-lime-300",
  },
] as const;

export const bestSellers = [
  { id: "best-1", brand: "NORTHLUME", name: "Áo khoác dạ dáng ngắn Essential", price: 890000, oldPrice: 1090000, rating: 4.9, sold: "1,2k", badge: "Giảm 18%", image: "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=900&q=80" },
  { id: "best-2", brand: "CASIO", name: "Đồng hồ kim dây thép không gỉ", price: 1590000, oldPrice: 1890000, rating: 4.8, sold: "860", badge: "Giảm 16%", image: "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=900&q=80" },
  { id: "best-3", brand: "URBAN PACE", name: "Giày sneaker da phối màu cổ điển", price: 1250000, oldPrice: 1490000, rating: 4.9, sold: "2,4k", badge: "Giảm 16%", image: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80" },
  { id: "best-4", brand: "LUNARO", name: "Túi da đeo vai Everyday Mini", price: 760000, oldPrice: 920000, rating: 4.7, sold: "730", badge: "Giảm 17%", image: "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=80" },
] as const;

export const newArrivals = [
  { id: "new-1", brand: "VERDE", name: "Áo thun cotton form rộng", price: 329000, oldPrice: 390000, rating: 4.8, sold: "420", badge: "Giảm 15%", image: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80" },
  { id: "new-2", brand: "ORION", name: "Kính mắt gọng vuông tối giản", price: 490000, oldPrice: 0, rating: 4.6, sold: "180", badge: "Limited", image: "https://images.unsplash.com/photo-1511499767150-a48a237f0083?auto=format&fit=crop&w=900&q=80" },
  { id: "new-3", brand: "NOMAD", name: "Dép sandal quai ngang unisex", price: 420000, oldPrice: 490000, rating: 4.8, sold: "510", badge: "Giảm 14%", image: "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&w=900&q=80" },
  { id: "new-4", brand: "AURELIA", name: "Ví da mini cầm tay", price: 380000, oldPrice: 450000, rating: 4.7, sold: "290", badge: "Giảm 16%", image: "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=900&q=80" },
] as const;
