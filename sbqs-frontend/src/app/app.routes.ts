import { Routes } from '@angular/router';

import { Home } from './pages/home/home';
import { BranchSelection } from './pages/branch-selection/branch-selection';
import { ServiceSelection } from './pages/service-selection/service-selection';
import { TicketResult } from './pages/ticket-result/ticket-result';
import { QueueMonitorComponent } from './pages/queue-monitor/queue-monitor';
import { StaffDashboard } from './pages/staff-dashboard/staff-dashboard';
import { AdminServices } from './pages/admin-services/admin-services';
import { AdminMappings } from './pages/admin-mappings/admin-mappings';
import { AdminBranchSetup } from './pages/admin-branch-setup/admin-branch-setup';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'branches', component: BranchSelection },
  { path: 'services', component: ServiceSelection },
  { path: 'ticket', component: TicketResult },
  { path: 'monitor', component: QueueMonitorComponent },
  { path: 'staff', component: StaffDashboard },
  {path: 'admin/services',component: AdminServices},
  {path: 'admin/mappings',component: AdminMappings},
  {path: 'admin/branch-setup',component: AdminBranchSetup}
];