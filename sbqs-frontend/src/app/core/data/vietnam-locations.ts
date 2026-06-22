export interface WardOption {
  label: string;
}

export interface DistrictOption {
  label: string;
  code: string;
  latitude: number;
  longitude: number;
  wards: WardOption[];
}

export interface ProvinceOption {
  label: string;
  code: string;
  districts: DistrictOption[];
}

const wards = (...labels: string[]): WardOption[] => labels.map((label) => ({ label }));

export const VIETNAM_LOCATIONS: ProvinceOption[] = [
  {
    label: 'Bình Dương', code: 'BD', districts: [
      { label: 'Thủ Dầu Một', code: 'TDM', latitude: 10.9804, longitude: 106.6519, wards: wards('Phú Cường', 'Phú Hòa', 'Phú Lợi', 'Hiệp Thành', 'Chánh Nghĩa', 'Định Hòa', 'Tương Bình Hiệp', 'Phú Mỹ', 'Hòa Phú', 'Hiệp An') },
      { label: 'Dĩ An', code: 'DA', latitude: 10.9068, longitude: 106.7694, wards: wards('Dĩ An', 'An Bình', 'Bình An', 'Bình Thắng', 'Đông Hòa', 'Tân Bình', 'Tân Đông Hiệp') },
      { label: 'Thuận An', code: 'TA', latitude: 10.9318, longitude: 106.7117, wards: wards('Lái Thiêu', 'An Phú', 'An Thạnh', 'Bình Chuẩn', 'Bình Hòa', 'Bình Nhâm', 'Hưng Định', 'Thuận Giao', 'Vĩnh Phú') },
      { label: 'Bến Cát', code: 'BC', latitude: 11.1510, longitude: 106.5940, wards: wards('Mỹ Phước', 'Chánh Phú Hòa', 'Hòa Lợi', 'Tân Định', 'Thới Hòa') },
      { label: 'Tân Uyên', code: 'TU', latitude: 11.0491, longitude: 106.7578, wards: wards('Uyên Hưng', 'Khánh Bình', 'Hội Nghĩa', 'Phú Chánh', 'Tân Hiệp', 'Tân Phước Khánh', 'Thái Hòa') },
      { label: 'Bàu Bàng', code: 'BB', latitude: 11.2872, longitude: 106.6153, wards: wards('Lai Uyên', 'Lai Hưng', 'Long Nguyên', 'Tân Hưng', 'Trừ Văn Thố') },
      { label: 'Bắc Tân Uyên', code: 'BTU', latitude: 11.1372, longitude: 106.8354, wards: wards('Tân Thành', 'Bình Mỹ', 'Đất Cuốc', 'Hiếu Liêm', 'Lạc An', 'Tân Bình') },
      { label: 'Phú Giáo', code: 'PG', latitude: 11.2912, longitude: 106.7942, wards: wards('Phước Vĩnh', 'An Bình', 'An Linh', 'Phước Hòa', 'Tân Hiệp', 'Vĩnh Hòa') },
      { label: 'Dầu Tiếng', code: 'DT', latitude: 11.3480, longitude: 106.4644, wards: wards('Dầu Tiếng', 'An Lập', 'Định An', 'Định Hiệp', 'Long Hòa', 'Minh Hòa') },
    ],
  },
  {
    label: 'Thành phố Hồ Chí Minh', code: 'HCM', districts: [
      { label: 'Quận 1', code: 'Q1', latitude: 10.7756, longitude: 106.7004, wards: wards('Bến Nghé', 'Bến Thành', 'Cầu Kho', 'Cầu Ông Lãnh', 'Đa Kao', 'Nguyễn Cư Trinh', 'Nguyễn Thái Bình', 'Tân Định') },
      { label: 'Quận 3', code: 'Q3', latitude: 10.7844, longitude: 106.6844, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 9', 'Võ Thị Sáu') },
      { label: 'Quận 7', code: 'Q7', latitude: 10.7340, longitude: 106.7216, wards: wards('Tân Phú', 'Tân Phong', 'Tân Quy', 'Tân Hưng', 'Tân Thuận Đông', 'Tân Thuận Tây', 'Phú Mỹ', 'Phú Thuận') },
      { label: 'Thành phố Thủ Đức', code: 'TD', latitude: 10.8494, longitude: 106.7537, wards: wards('An Khánh', 'An Phú', 'Bình Thọ', 'Hiệp Bình Chánh', 'Linh Trung', 'Linh Tây', 'Long Bình', 'Thảo Điền', 'Thủ Thiêm') },
      { label: 'Quận Bình Thạnh', code: 'BT', latitude: 10.8106, longitude: 106.7091, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 5', 'Phường 11', 'Phường 13', 'Phường 19', 'Phường 22', 'Phường 25') },
    ],
  },
  {
    label: 'Đồng Nai', code: 'DNA', districts: [
      { label: 'Biên Hòa', code: 'BH', latitude: 10.9574, longitude: 106.8426, wards: wards('An Bình', 'Bửu Hòa', 'Bửu Long', 'Hố Nai', 'Long Bình', 'Quang Vinh', 'Quyết Thắng', 'Tam Hiệp', 'Tân Phong', 'Trảng Dài') },
      { label: 'Long Khánh', code: 'LK', latitude: 10.9265, longitude: 107.2442, wards: wards('Bảo Vinh', 'Bàu Sen', 'Phú Bình', 'Suối Tre', 'Xuân An', 'Xuân Bình', 'Xuân Hòa', 'Xuân Thanh') },
      { label: 'Nhơn Trạch', code: 'NT', latitude: 10.6969, longitude: 106.8898, wards: wards('Hiệp Phước', 'Long Tân', 'Long Thọ', 'Phú Hội', 'Phước An', 'Phước Thiền') },
    ],
  },
  {
    label: 'Bà Rịa - Vũng Tàu', code: 'BRVT', districts: [
      { label: 'Vũng Tàu', code: 'VT', latitude: 10.4114, longitude: 107.1362, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 7', 'Phường 8', 'Phường 10', 'Phường 11', 'Thắng Tam', 'Rạch Dừa') },
      { label: 'Bà Rịa', code: 'BR', latitude: 10.4990, longitude: 107.1676, wards: wards('Phước Hiệp', 'Phước Hưng', 'Phước Nguyên', 'Long Hương', 'Long Tâm', 'Kim Dinh') },
    ],
  },
  {
    label: 'Hà Nội', code: 'HN', districts: [
      { label: 'Hoàn Kiếm', code: 'HK', latitude: 21.0287, longitude: 105.8522, wards: wards('Hàng Bạc', 'Hàng Bài', 'Hàng Bồ', 'Hàng Buồm', 'Hàng Đào', 'Tràng Tiền', 'Cửa Đông') },
      { label: 'Cầu Giấy', code: 'CG', latitude: 21.0362, longitude: 105.7906, wards: wards('Dịch Vọng', 'Dịch Vọng Hậu', 'Mai Dịch', 'Nghĩa Đô', 'Nghĩa Tân', 'Quan Hoa', 'Trung Hòa', 'Yên Hòa') },
      { label: 'Ba Đình', code: 'BDI', latitude: 21.0358, longitude: 105.8347, wards: wards('Cống Vị', 'Điện Biên', 'Đội Cấn', 'Giảng Võ', 'Kim Mã', 'Liễu Giai', 'Ngọc Hà', 'Quán Thánh') },
    ],
  },
  {
    label: 'Đà Nẵng', code: 'DN', districts: [
      { label: 'Hải Châu', code: 'HC', latitude: 16.0471, longitude: 108.2068, wards: wards('Bình Hiên', 'Bình Thuận', 'Hải Châu I', 'Hải Châu II', 'Hòa Cường Bắc', 'Hòa Cường Nam', 'Nam Dương', 'Phước Ninh', 'Thạch Thang') },
      { label: 'Sơn Trà', code: 'ST', latitude: 16.1063, longitude: 108.2520, wards: wards('An Hải Bắc', 'An Hải Đông', 'An Hải Tây', 'Mân Thái', 'Nại Hiên Đông', 'Phước Mỹ', 'Thọ Quang') },
    ],
  },
  {
    label: 'Cần Thơ', code: 'CT', districts: [
      { label: 'Ninh Kiều', code: 'NK', latitude: 10.0342, longitude: 105.7594, wards: wards('An Cư', 'An Hòa', 'An Khánh', 'An Nghiệp', 'Cái Khế', 'Hưng Lợi', 'Tân An', 'Thới Bình', 'Xuân Khánh') },
      { label: 'Bình Thủy', code: 'BTH', latitude: 10.0743, longitude: 105.7396, wards: wards('An Thới', 'Bình Thủy', 'Bùi Hữu Nghĩa', 'Long Hòa', 'Long Tuyền', 'Thới An Đông') },
    ],
  },
];
