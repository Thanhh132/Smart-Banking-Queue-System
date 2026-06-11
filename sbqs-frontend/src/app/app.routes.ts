import { Routes } from '@angular/router';

import { Home } from './pages/home/home';
import { BranchSelection } from './pages/branch-selection/branch-selection';
import { ServiceSelection } from './pages/service-selection/service-selection';
import { TicketResult } from './pages/ticket-result/ticket-result';
import { QueueMonitorComponent } from './pages/queue-monitor/queue-monitor';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'branches', component: BranchSelection },
  { path: 'services', component: ServiceSelection },
  { path: 'ticket', component: TicketResult },
  {
    path: 'monitor',
    component: QueueMonitorComponent
  }
];