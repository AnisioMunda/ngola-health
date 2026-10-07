import { Routes } from '@angular/router';

// Guard simples para o portal
export const portalAuthGuard = () => {
  const token = localStorage.getItem('portal_token');
  if (token) return true;
  window.location.href = '/portal/login';
  return false;
};

export const portalPublicGuard = () => {
  const token = localStorage.getItem('portal_token');
  if (!token) return true;
  window.location.href = '/portal';
  return false;
};

export const portalRoutes: Routes = [
  {
    path: 'portal',
    children: [
      {
        path: 'login',
        canActivate: [portalPublicGuard],
        loadComponent: () => import('./features/portal/portal-login/portal-login.component')
          .then(m => m.PortalLoginComponent)
      },
      {
        path: '',
        canActivate: [portalAuthGuard],
        loadComponent: () => import('./features/portal/portal-dashboard/portal-dashboard.component')
          .then(m => m.PortalDashboardComponent)
      }
    ]
  }
];