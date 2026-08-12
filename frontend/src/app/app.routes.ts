import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { adminGuard } from './core/guards/admin.guard';
import { AppShell } from './layout/app-shell';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/public/landing/landing').then((m) => m.Landing)
  },
  {
    path: 'features',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'FEATURES' }
  },
  {
    path: 'security',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'SECURITY' }
  },
  {
    path: 'about',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'ABOUT' }
  },
  {
    path: 'help',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'HELP' }
  },
  {
    path: 'contact',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'CONTACT' }
  },
  {
    path: 'privacy',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'PRIVACY' }
  },
  {
    path: 'terms',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'TERMS' }
  },
  {
    path: 'accessibility',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'ACCESSIBILITY' }
  },
  {
    path: 'disclaimer',
    loadComponent: () => import('./features/public/content-page/content-page').then((m) => m.ContentPage),
    data: { pageKey: 'DISCLAIMER' }
  },
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
      {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard)
      },
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
          },
          {
            path: 'experience',
            loadComponent: () => import('./features/admin/site-experience-admin/site-experience-admin').then((m) => m.SiteExperienceAdmin)
          },
          {
            path: 'content',
            loadComponent: () => import('./features/admin/site-content-admin/site-content-admin').then((m) => m.SiteContentAdmin)
          },
          {
            path: 'announcements',
            loadComponent: () => import('./features/admin/site-announcements-admin/site-announcements-admin').then((m) => m.SiteAnnouncementsAdmin)
          },
          {
            path: 'media',
            loadComponent: () => import('./features/admin/site-media-admin/site-media-admin').then((m) => m.SiteMediaAdmin)
          }
        ]
      }
    ]
  },
  { path: '**', redirectTo: 'app' }
];
