import { Routes } from '@angular/router';

import { Home } from './pages/public/home/home';
import { Login } from './pages/auth/login/login';
import { Register } from './pages/auth/register/register';
import { Customer } from './pages/customer/portal/customer';
import { BranchSelection } from './pages/customer/branch-selection/branch-selection';
import { ServiceSelection } from './pages/customer/service-selection/service-selection';
import { TicketResult } from './pages/customer/ticket-result/ticket-result';
import { QueueMonitorComponent } from './pages/queue/monitor/queue-monitor';
import { StaffDashboard } from './pages/staff/dashboard/staff-dashboard';
import { AdminDashboard } from './pages/admin/dashboard/admin-dashboard';
import { AdminServices } from './pages/admin/services/admin-services';
import { AdminMappings } from './pages/admin/mappings/admin-mappings';
import { AdminUsers } from './pages/admin/users/admin-users';
import { SuperAdmin } from './pages/super-admin/dashboard/super-admin';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'home', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'customer', component: Customer, canActivate: [roleGuard(['CUSTOMER'])] },
  { path: 'branches', component: BranchSelection, canActivate: [roleGuard(['CUSTOMER'])] },
  { path: 'services', component: ServiceSelection, canActivate: [roleGuard(['CUSTOMER'])] },
  { path: 'ticket', component: TicketResult, canActivate: [roleGuard(['CUSTOMER'])] },
  { path: 'monitor', component: QueueMonitorComponent, canActivate: [roleGuard(['BRANCH_ADMIN', 'STAFF'])] },
  { path: 'staff', component: StaffDashboard, canActivate: [roleGuard(['STAFF'])] },
  { path: 'admin', component: AdminDashboard, canActivate: [roleGuard(['BRANCH_ADMIN'])] },
  { path: 'admin/services', component: AdminServices, canActivate: [roleGuard(['BRANCH_ADMIN'])] },
  { path: 'admin/mappings', component: AdminMappings, canActivate: [roleGuard(['BRANCH_ADMIN'])] },
  { path: 'admin/users', component: AdminUsers, canActivate: [roleGuard(['BRANCH_ADMIN'])] },
  { path: 'super-admin', component: SuperAdmin, canActivate: [roleGuard(['SUPER_ADMIN'])] },
];
