import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { adminGuard } from './core/guards/admin.guard';
import { AppShell } from './layout/app-shell';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register)
  },
  {
    path: 'app',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'urls' },
      {
        path: 'urls',
        loadComponent: () => import('./features/urls/url-dashboard/url-dashboard').then((m) => m.UrlDashboard)
      },
      {
        path: 'urls/new',
        loadComponent: () => import('./features/urls/url-create/url-create').then((m) => m.UrlCreate)
      },
      {
        path: 'urls/:id',
        loadComponent: () => import('./features/urls/url-details/url-details').then((m) => m.UrlDetails)
      },
      {
        path: 'urls/:id/analytics',
        loadComponent: () => import('./features/analytics/url-analytics/url-analytics').then((m) => m.UrlAnalytics)
      },
      {
        path: 'campaigns',
        loadComponent: () => import('./features/campaigns/campaigns').then((m) => m.Campaigns)
      },
      {
        path: 'audit',
        loadComponent: () => import('./features/audit/workspace-audit/workspace-audit').then((m) => m.WorkspaceAudit)
      },
      {
        path: 'api-keys',
        loadComponent: () => import('./features/api-keys/api-keys').then((m) => m.ApiKeys)
      },
      {
        path: 'workspace',
        loadComponent: () => import('./features/workspace/workspace-management').then((m) => m.WorkspaceManagement)
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'overview' },
          {
            path: 'overview',
            loadComponent: () => import('./features/admin/admin-overview/admin-overview').then((m) => m.AdminOverview)
          },
          {
            path: 'moderation',
            loadComponent: () => import('./features/admin/admin-moderation/admin-moderation').then((m) => m.AdminModeration)
          },
          {
            path: 'audit',
            loadComponent: () => import('./features/admin/admin-audit/admin-audit').then((m) => m.AdminAudit)
          },
          {
            path: 'outbox',
            loadComponent: () => import('./features/admin/admin-outbox/admin-outbox').then((m) => m.AdminOutbox)
          }
        ]
      }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'app' },
  { path: '**', redirectTo: 'app' }
];
