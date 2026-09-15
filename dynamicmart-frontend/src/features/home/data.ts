export const categories = [
  { icon: "shirt", name: "Thời trang nam", caption: "1.240 sản phẩm", href: "/products?category=thoi-trang-nam", color: "from-sky-100 to-blue-50 text-sky-700" },
  { icon: "sparkles", name: "Thời trang nữ", caption: "1.860 sản phẩm", href: "/products?category=thoi-trang-nu", color: "from-rose-100 to-pink-50 text-rose-700" },
  { icon: "watch", name: "Đồng hồ", caption: "430 sản phẩm", href: "/products?category=dong-ho", color: "from-amber-100 to-orange-50 text-amber-700" },
  { icon: "footprints", name: "Giày dép", caption: "920 sản phẩm", href: "/products?category=giay-dep", color: "from-violet-100 to-purple-50 text-violet-700" },
  { icon: "shoppingBag", name: "Túi & phụ kiện", caption: "760 sản phẩm", href: "/products?category=tui-phu-kien", color: "from-emerald-100 to-teal-50 text-emerald-700" },
  { icon: "gift", name: "Quà tặng", caption: "350 sản phẩm", href: "/products?category=qua-tang", color: "from-red-100 to-orange-50 text-red-700" },
  { icon: "gem", name: "Trang sức", caption: "280 sản phẩm", href: "/products?category=trang-suc", color: "from-fuchsia-100 to-pink-50 text-fuchsia-700" },
  { icon: "headphones", name: "Công nghệ", caption: "540 sản phẩm", href: "/products?category=cong-nghe", color: "from-cyan-100 to-sky-50 text-cyan-700" },
  { icon: "house", name: "Nhà cửa", caption: "680 sản phẩm", href: "/products?category=nha-cua", color: "from-lime-100 to-green-50 text-lime-700" },
  { icon: "dumbbell", name: "Thể thao", caption: "510 sản phẩm", href: "/products?category=the-thao", color: "from-indigo-100 to-blue-50 text-indigo-700" },
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
    id: "new-season",
    eyebrow: "New season · 2026",
    title: "Chạm chất riêng, mở lối phong cách.",
    description: "Những thiết kế mới nhất được tuyển chọn cho nhịp sống hiện đại — tinh tế, linh hoạt và luôn khác biệt.",
    primaryLabel: "Khám phá bộ sưu tập",
    primaryHref: "/products?sort=newest",
    secondaryLabel: "Xem bán chạy",
    secondaryHref: "#best-seller",
    image: "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1800&q=88",
    accent: "text-emerald-300",
  },
  {
    id: "timepieces",
    eyebrow: "The watch edit",
    title: "Đúng khoảnh khắc. Đúng khí chất.",
    description: "Tuyển chọn đồng hồ chính hãng từ những thương hiệu được yêu thích, dành cho mọi dấu mốc đáng nhớ.",
    primaryLabel: "Mua đồng hồ",
    primaryHref: "/products?category=dong-ho",
    secondaryLabel: "Khám phá thương hiệu",
    secondaryHref: "#brands",
    image: "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=1800&q=88",
    accent: "text-amber-300",
  },
  {
    id: "street-style",
    eyebrow: "Street essentials",
    title: "Bước ra phố với phiên bản tốt nhất.",
    description: "Sneaker, phụ kiện và những món đồ thiết yếu giúp bạn hoàn thiện diện mạo theo cách riêng.",
    primaryLabel: "Khám phá ngay",
    primaryHref: "/products?category=giay-dep",
    secondaryLabel: "Ưu đãi hôm nay",
    secondaryHref: "/products?promotion=true",
    image: "https://images.unsplash.com/photo-1495555961986-6d4c1ecb7be3?auto=format&fit=crop&w=1800&q=88",
    accent: "text-sky-300",
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
