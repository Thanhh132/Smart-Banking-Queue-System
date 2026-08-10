export interface ServingCounter {
  counterName: string;
  ticketNumber: number | null;
  status: string;
  queueMachineName?: string | null;
  staffName?: string | null;
}

export interface QueueMonitor {
  branchName: string;
  servingCounters: ServingCounter[];
  waitingCount: number;
}
