import { Routes } from '@angular/router';

import { roleGuard } from './core/guards/role.guard';
import { homeRedirectGuard } from './core/guards/home-redirect.guard';
import { customerProfileGuard } from './core/guards/customer-profile.guard';
import { branchOpenGuard } from './core/guards/branch-open.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'home', loadComponent: () => import('./pages/public/home/home').then((m) => m.Home) },
  { path: 'login', loadComponent: () => import('./pages/auth/login/login').then((m) => m.Login) },
  {
    path: 'auth/google/callback',
    loadComponent: () =>
      import('./pages/auth/google-callback/google-callback').then((m) => m.GoogleCallback),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./pages/auth/verify-email/verify-email').then((m) => m.VerifyEmail),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./pages/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./pages/auth/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'confirm-account-change',
    loadComponent: () =>
      import('./pages/account/confirm-account-change').then((m) => m.ConfirmAccountChange),
  },
  {
    path: 'account',
    loadComponent: () =>
      import('./pages/account/account').then((m) => m.Account),
    canActivate: [roleGuard(['CUSTOMER', 'STAFF', 'BRANCH_ADMIN', 'SUPER_ADMIN'])],
  },
  {
    path: 'complete-profile',
    loadComponent: () =>
      import('./pages/account/complete-profile/complete-profile').then((m) => m.CompleteProfile),
    canActivate: [roleGuard(['CUSTOMER'])],
  },
  {
    path: 'customer',
    loadComponent: () => import('./pages/customer/portal/customer').then((m) => m.Customer),
    canActivate: [roleGuard(['CUSTOMER']), customerProfileGuard],
  },
  {
    path: 'branches',
    loadComponent: () =>
      import('./pages/customer/branch-selection/branch-selection').then((m) => m.BranchSelection),
    canActivate: [roleGuard(['CUSTOMER']), customerProfileGuard],
  },
  {
    path: 'services',
    loadComponent: () =>
      import('./pages/customer/service-selection/service-selection').then(
        (m) => m.ServiceSelection,
      ),
    canActivate: [roleGuard(['CUSTOMER']), customerProfileGuard, branchOpenGuard],
  },
  {
    path: 'ticket',
    loadComponent: () =>
      import('./pages/customer/ticket-result/ticket-result').then((m) => m.TicketResult),
    canActivate: [roleGuard(['CUSTOMER']), customerProfileGuard],
  },
  {
    path: 'delegations',
    loadComponent: () => import('./pages/customer/delegations/customer-delegations').then((m) => m.CustomerDelegations),
    canActivate: [roleGuard(['CUSTOMER']), customerProfileGuard],
  },
  {
    path: 'monitor',
    loadComponent: () =>
      import('./pages/queue/monitor/queue-monitor').then((m) => m.QueueMonitorComponent),
    canActivate: [roleGuard(['BRANCH_ADMIN', 'STAFF'])],
  },
  {
    path: 'staff',
    loadComponent: () =>
      import('./pages/staff/dashboard/staff-dashboard').then((m) => m.StaffDashboard),
    canActivate: [roleGuard(['STAFF'])],
  },
  {
    path: 'staff/history',
    loadComponent: () =>
      import('./pages/staff/history/staff-history').then((m) => m.StaffHistory),
    canActivate: [roleGuard(['STAFF'])],
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./pages/admin/dashboard/admin-dashboard').then((m) => m.AdminDashboard),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'admin/operations',
    loadComponent: () =>
      import('./pages/admin/operations/admin-operations').then((m) => m.AdminOperations),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'admin/services',
    loadComponent: () =>
      import('./pages/admin/services/admin-services').then((m) => m.AdminServices),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'admin/mappings',
    loadComponent: () =>
      import('./pages/admin/mappings/admin-mappings').then((m) => m.AdminMappings),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./pages/admin/users/admin-users').then((m) => m.AdminUsers),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'admin/history',
    loadComponent: () =>
      import('./pages/admin/history/admin-history').then((m) => m.AdminHistory),
    canActivate: [roleGuard(['BRANCH_ADMIN'])],
  },
  {
    path: 'super-admin',
    loadComponent: () =>
      import('./pages/super-admin/dashboard/super-admin').then((m) => m.SuperAdmin),
    canActivate: [roleGuard(['SUPER_ADMIN'])],
  },
  {
    path: 'super-admin/branches',
    loadComponent: () =>
      import('./pages/super-admin/branches/super-admin-branches').then((m) => m.SuperAdminBranches),
    canActivate: [roleGuard(['SUPER_ADMIN'])],
  },
  {
    path: 'super-admin/services',
    loadComponent: () => import('./pages/super-admin/services/super-admin-services').then((m) => m.SuperAdminServices),
    canActivate: [roleGuard(['SUPER_ADMIN'])],
  },
  {
    path: '**',
    canActivate: [homeRedirectGuard],
    loadComponent: () => import('./pages/public/home/home').then((m) => m.Home),
  },
];
