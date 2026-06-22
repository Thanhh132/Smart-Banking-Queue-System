export interface Branch {
  branchId: number;
  bankName: string;
  branchCode: string;
  branchName: string;
  province?: string;
  district?: string;
  ward?: string;
  address: string;
  phone: string;
  status: string;
  latitude?: number;
  longitude?: number;
}
