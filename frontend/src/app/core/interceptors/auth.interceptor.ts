import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const SESSION_ENDPOINTS = ['/auth/login', '/auth/refresh', '/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const path = request.url.split(/[?#]/, 1)[0];

  if (isSessionEndpoint(path) || path.includes('/portal/')) {
    return next(request);
  }

  const accessToken = authService.getAccessToken();
  const authenticatedRequest = withAccessToken(request, accessToken);

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      if (!authService.getRefreshToken()) {
        expireSession(authService, router);
        return throwError(() => error);
      }

      return authService.refreshSession().pipe(
        catchError((refreshError: HttpErrorResponse) => {
          if ([400, 401, 403].includes(refreshError.status)) {
            expireSession(authService, router);
          }
          return throwError(() => refreshError);
        }),
        switchMap((response) =>
          next(withAccessToken(request, response.accessToken)).pipe(
            catchError((retryError: HttpErrorResponse) => {
              if (retryError.status === 401) {
                expireSession(authService, router);
              }
              return throwError(() => retryError);
            }),
          ),
        ),
      );
    }),
  );
};

function withAccessToken(
  request: HttpRequest<unknown>,
  token: string | null,
): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

function isSessionEndpoint(path: string): boolean {
  return SESSION_ENDPOINTS.some((endpoint) => path.endsWith(endpoint));
}

function expireSession(authService: AuthService, router: Router): void {
  const currentUrl = router.url;
  authService.clearSession();
  const queryParams =
    currentUrl && currentUrl !== '/' && !currentUrl.startsWith('/login')
      ? { returnUrl: currentUrl }
      : undefined;
  void router.navigate(['/login'], { queryParams, replaceUrl: true });
}
