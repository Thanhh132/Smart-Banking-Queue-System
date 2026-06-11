export interface ServingCounter {
  counterName: string;
  ticketNumber: number;
}

export interface QueueMonitor {
  branchName: string;
  servingCounters: ServingCounter[];
  waitingCount: number;
}