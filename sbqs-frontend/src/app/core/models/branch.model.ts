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

export interface SmartBranchRecommendation extends Branch {
  rank: number;
  recommended: boolean;
  distanceKm: number;
  waitingTickets: number;
  activeCounters: number;
  estimatedWaitMinutes: number;
  distanceScore: number;
  waitScore: number;
  routingScore: number;
  explanation: string;
  calculatedAt: string;
}
