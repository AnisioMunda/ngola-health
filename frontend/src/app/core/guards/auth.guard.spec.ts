import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard, publicGuard } from './auth.guard';

describe('authentication guards', () => {
  let isLoggedIn: jasmine.Spy;
  let router: Router;

  beforeEach(() => {
    isLoggedIn = jasmine.createSpy('isLoggedIn').and.returnValue(false);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { isLoggedIn } }],
    });
    router = TestBed.inject(Router);
  });

  it('redirects unauthenticated users to login with the original route', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard(
        {} as ActivatedRouteSnapshot,
        { url: '/patients/42?tab=history' } as RouterStateSnapshot,
      ),
    );

    if (!(result instanceof UrlTree)) {
      throw new Error('Expected the guard to return a login UrlTree.');
    }
    expect(result.queryParams['returnUrl']).toBe('/patients/42?tab=history');
  });

  it('lets authenticated users through protected routes', () => {
    isLoggedIn.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/dashboard' } as RouterStateSnapshot),
    );

    expect(result).toBeTrue();
  });

  it('redirects authenticated users away from the login page', () => {
    isLoggedIn.and.returnValue(true);
    const result = TestBed.runInInjectionContext(() =>
      publicGuard({} as ActivatedRouteSnapshot, { url: '/login' } as RouterStateSnapshot),
    );

    if (!(result instanceof UrlTree)) {
      throw new Error('Expected the guard to return the dashboard UrlTree.');
    }
    expect(router.serializeUrl(result)).toBe('/dashboard');
  });
});
