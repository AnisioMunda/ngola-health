import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard funcional (Angular 17+).
 * Protege rotas que requerem autenticação.
 * Se o utilizador não tiver token válido, redirige para /login.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Guard para operações de gestão de utilizadores, reservadas ao administrador do hospital.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.getCurrentUser()?.roles?.includes('ADMIN')
    ? true
    : router.createUrlTree(['/users']);
};

/**
 * Guard para a gestão global de hospitais, reservada ao super-administrador da plataforma.
 */
export const superAdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.getCurrentUser()?.roles?.includes('SUPER_ADMIN')
    ? true
    : router.createUrlTree(['/dashboard']);
};

/**
 * Guard para redirigir utilizadores já autenticados.
 * Usado na rota /login — se já estiver autenticado,
 * vai directamente para o dashboard.
 */
export const publicGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isLoggedIn() ? router.createUrlTree(['/dashboard']) : true;
};
