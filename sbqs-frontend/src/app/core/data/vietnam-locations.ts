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
    label: 'Thành phố Hà Nội',
    code: '01',
    districts: [
      { label: 'Quận Ba Đình', code: '001', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Phúc Xá', 'Phường Trúc Bạch', 'Phường Vĩnh Phúc', 'Phường Cống Vị', 'Phường Giảng Võ', 'Phường Thành Công', 'Phường Kim Mã', 'Phường Ngọc Khánh', 'Phường Đội Cấn', 'Phường Điện Biên', 'Phường Quán Thánh', 'Phường Nguyễn Trung Trực', 'Phường Liễu Giai') },
      { label: 'Quận Hoàn Kiếm', code: '002', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Phúc Tân', 'Phường Đồng Xuân', 'Phường Hàng Mã', 'Phường Hàng Buồm', 'Phường Hàng Đào', 'Phường Hàng Bạc', 'Phường Hàng Gai', 'Phường Cửa Đông', 'Phường Hàng Bông', 'Phường Tràng Tiền', 'Phường Trần Hưng Đạo', 'Phường Phan Chu Trinh', 'Phường Hàng Bài', 'Phường Lý Thái Tổ', 'Phường Cửa Nam', 'Phường Hàng Trống', 'Phường Hàng Khay', 'Phường Chương Dương') },
      { label: 'Quận Tây Hồ', code: '003', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Phú Thượng', 'Phường Nhật Tân', 'Phường Tứ Liên', 'Phường Quảng An', 'Phường Xuân La', 'Phường Yên Phụ', 'Phường Bưởi', 'Phường Thụy Khuê') },
      { label: 'Quận Cầu Giấy', code: '005', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Nghĩa Đô', 'Phường Nghĩa Tân', 'Phường Dịch Vọng', 'Phường Dịch Vọng Hậu', 'Phường Quan Hoa', 'Phường Láng Thượng', 'Phường Trung Hòa', 'Phường Yên Hòa', 'Phường Mai Dịch') },
      { label: 'Quận Đống Đa', code: '006', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Cát Linh', 'Phường Văn Miếu', 'Phường Quốc Tử Giám', 'Phường Hàng Bột', 'Phường Ô Chợ Dừa', 'Phường Thổ Quan', 'Phường Khâm Thiên', 'Phường Trung Phụng', 'Phường Quang Trung', 'Phường Khương Thượng', 'Phường Nam Đồng', 'Phường Phương Liên', 'Phường Phương Mai', 'Phường Khương Mai', 'Phường Láng Hạ', 'Phường Láng Thượng', 'Phường Kim Liên', 'Phường Trung Tự', 'Phường Trung Liệt', 'Phường Thịnh Quang') },
      { label: 'Quận Hai Bà Trưng', code: '007', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Nguyễn Du', 'Phường Lê Đại Hành', 'Phường Phố Huế', 'Phường Phạm Đình Hổ', 'Phường Bạch Đằng', 'Phường Thanh Lương', 'Phường Thanh Nhàn', 'Phường Cầu Dền', 'Phường Bách Khoa', 'Phường Đồng Tâm', 'Phường Quỳnh Lôi', 'Phường Quỳnh Mai', 'Phường Vĩnh Tuy', 'Phường Minh Khai', 'Phường Trương Định') },
      { label: 'Quận Thanh Xuân', code: '009', latitude: 21.0285, longitude: 105.8542, wards: wards('Phường Nhân Chính', 'Phường Thượng Đình', 'Phường Khương Trung', 'Phường Khương Mai', 'Phường Khương Đình', 'Phường Hạ Đình', 'Phường Thanh Xuân Bắc', 'Phường Thanh Xuân Nam', 'Phường Thanh Xuân Trung', 'Phường Phương Liệt', 'Phường Kim Giang') },
    ],
  },
  {
    label: 'Thành phố Hồ Chí Minh',
    code: '79',
    districts: [
      { label: 'Quận 1', code: '760', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường Bến Nghé', 'Phường Bến Thành', 'Phường Cô Giang', 'Phường Cầu Kho', 'Phường Cầu Ông Lãnh', 'Phường Nguyễn Cư Trinh', 'Phường Nguyễn Thái Bình', 'Phường Phạm Ngũ Lão', 'Phường Tân Định', 'Phường Đa Kao') },
      { label: 'Quận 3', code: '770', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường Võ Thị Sáu', 'Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 9', 'Phường 10', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14') },
      { label: 'Quận 10', code: '771', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 2', 'Phường 4', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 8', 'Phường 9', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14', 'Phường 15') },
      { label: 'Quận 11', code: '772', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 8', 'Phường 9', 'Phường 10', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14', 'Phường 15', 'Phường 16') },
      { label: 'Quận 5', code: '773', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 8', 'Phường 9', 'Phường 10', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14') },
      { label: 'Quận 6', code: '774', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 8', 'Phường 9', 'Phường 10', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14') },
      { label: 'Quận 7', code: '778', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường Tân Kiểng', 'Phường Tân Quy', 'Phường Tân Phong', 'Phường Tân Phú', 'Phường Tân Thuận Đông', 'Phường Tân Thuận Tây', 'Phường Bình Thuận', 'Phường Phú Mỹ', 'Phường Phú Thuận') },
      { label: 'Quận Gò Vấp', code: '764', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 3', 'Phường 4', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 8', 'Phường 9', 'Phường 10', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14', 'Phường 15', 'Phường 16', 'Phường 17') },
      { label: 'Quận Bình Thạnh', code: '765', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường 1', 'Phường 2', 'Phường 3', 'Phường 5', 'Phường 6', 'Phường 7', 'Phường 11', 'Phường 12', 'Phường 13', 'Phường 14', 'Phường 15', 'Phường 17', 'Phường 19', 'Phường 21', 'Phường 22', 'Phường 24', 'Phường 25', 'Phường 26', 'Phường 27', 'Phường 28') },
      { label: 'Thành phố Thủ Đức', code: '769', latitude: 10.7769, longitude: 106.7009, wards: wards('Phường Thảo Điền', 'Phường An Phú', 'Phường Bình An', 'Phường An Khánh', 'Phường Thủ Thiêm', 'Phường Linh Đông', 'Phường Linh Tây', 'Phường Linh Chiểu', 'Phường Linh Trung', 'Phường Linh Xuân', 'Phường Bình Thọ', 'Phường Trường Thọ', 'Phường Tam Phú', 'Phường Tam Bình', 'Phường Hiệp Bình Chánh', 'Phường Hiệp Bình Phước', 'Phường Long Trường', 'Phường Trường Thạnh', 'Phường Phước Long A', 'Phường Phước Long B', 'Phường Tăng Nhơn Phú A', 'Phường Tăng Nhơn Phú B') },
    ],
  },
  {
    label: 'Bình Dương',
    code: '74',
    districts: [
      { label: 'Thành phố Thủ Dầu Một', code: '718', latitude: 10.9804, longitude: 106.6519, wards: wards('Phường Phú Cường', 'Phường Hiệp Thành', 'Phường Chánh Nghĩa', 'Phường Phú Thọ', 'Phường Phú Hòa', 'Phường Phú Lợi', 'Phường Định Hòa', 'Phường Hiệp An', 'Phường Phú Mỹ', 'Phường Hòa Phú', 'Phường Phú Tân', 'Phường Tương Bình Hiệp', 'Phường Tân An', 'Phường Chánh Mỹ') },
      { label: 'Thành phố Thuận An', code: '721', latitude: 10.9804, longitude: 106.6519, wards: wards('Phường Lái Thiêu', 'Phường An Thạnh', 'Phường Vĩnh Phú', 'Phường Bình Hòa', 'Phường Bình Nhâm', 'Phường Thuận Giao', 'Phường An Phú', 'Phường Bình Chuẩn', 'Phường Hưng Định', 'Xã An Sơn') },
      { label: 'Thành phố Dĩ An', code: '722', latitude: 10.9804, longitude: 106.6519, wards: wards('Phường Dĩ An', 'Phường An Bình', 'Phường Tân Đông Hiệp', 'Phường Đông Hòa', 'Phường Tân Bình', 'Phường Bình An', 'Phường Bình Thắng') },
    ],
  },
  {
    label: 'Đồng Nai',
    code: '75',
    districts: [
      { label: 'Thành phố Biên Hòa', code: '731', latitude: 10.9574, longitude: 106.8426, wards: wards('Phường Thanh Bình', 'Phường Hòa Bình', 'Phường Quang Vinh', 'Phường Quyết Thắng', 'Phường Thống Nhất', 'Phường Trung Dũng', 'Phường Bửu Long', 'Phường Tân Tiến', 'Phường Tân Mai', 'Phường Tam Hiệp', 'Phường Tam Hòa', 'Phường Tân Hiệp', 'Phường Trảng Dài', 'Phường Tân Phong', 'Phường Hố Nai', 'Phường An Bình', 'Phường Bình Đa') },
    ],
  },
  {
    label: 'Thành phố Đà Nẵng',
    code: '48',
    districts: [
      { label: 'Quận Hải Châu', code: '490', latitude: 16.0471, longitude: 108.2068, wards: wards('Phường Hải Châu I', 'Phường Hải Châu II', 'Phường Thạch Thang', 'Phường Thanh Bình', 'Phường Thuận Phước', 'Phường Phước Ninh', 'Phường Nam Dương', 'Phường Bình Hiên', 'Phường Bình Thuận', 'Phường Hòa Thuận Đông', 'Phường Hòa Thuận Tây', 'Phường Hòa Cường Bắc', 'Phường Hòa Cường Nam') },
      { label: 'Quận Thanh Khê', code: '492', latitude: 16.0471, longitude: 108.2068, wards: wards('Phường Tam Thuận', 'Phường Thanh Khê Đông', 'Phường Thanh Khê Tây', 'Phường Xuân Hà', 'Phường Chính Gián', 'Phường Thạc Gián', 'Phường An Khê', 'Phường Hòa Khê', 'Phường Vĩnh Trung', 'Phường Tân Chính') },
    ],
  },
  {
    label: 'Quảng Ninh',
    code: '22',
    districts: [
      { label: 'Thành phố Hạ Long', code: '193', latitude: 20.9712, longitude: 107.0448, wards: wards('Phường Bạch Đằng', 'Phường Hòn Gai', 'Phường Bãi Cháy', 'Phường Hồng Gai', 'Phường Hồng Hà', 'Phường Hồng Hải', 'Phường Cao Thắng', 'Phường Cao Xanh', 'Phường Giếng Đáy', 'Phường Hà Tu', 'Phường Hà Khẩu', 'Phường Tuần Châu') },
    ],
  },
  {
    label: 'Nghệ An',
    code: '40',
    districts: [
      { label: 'Thành phố Vinh', code: '412', latitude: 18.6796, longitude: 105.6813, wards: wards('Phường Lê Mao', 'Phường Lê Lợi', 'Phường Hà Huy Tập', 'Phường Đội Cung', 'Phường Quang Trung', 'Phường Cửa Nam', 'Phường Trường Thi', 'Phường Hưng Bình', 'Phường Hưng Phúc', 'Phường Đông Vĩnh', 'Phường Bến Thủy') },
    ],
  },
];
