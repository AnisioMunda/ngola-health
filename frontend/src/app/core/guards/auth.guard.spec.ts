import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService, AuthUser } from '../services/auth.service';
import { adminGuard, authGuard, publicGuard } from './auth.guard';

describe('authentication guards', () => {
  let isLoggedIn: jasmine.Spy;
  let getCurrentUser: jasmine.Spy;
  let router: Router;

  beforeEach(() => {
    isLoggedIn = jasmine.createSpy('isLoggedIn').and.returnValue(false);
    getCurrentUser = jasmine.createSpy('getCurrentUser').and.returnValue(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isLoggedIn, getCurrentUser } },
      ],
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

  it('allows user-management mutations only for hospital administrators', () => {
    getCurrentUser.and.returnValue(createUser(['ADMIN']));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    expect(result).toBeTrue();
  });

  it('redirects non-administrators to the user list', () => {
    getCurrentUser.and.returnValue(createUser(['MANAGER']));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

    if (!(result instanceof UrlTree)) {
      throw new Error('Expected the guard to return the user list UrlTree.');
    }
    expect(router.serializeUrl(result)).toBe('/users');
  });
});

function createUser(roles: string[]): AuthUser {
  return {
    id: 'user-id',
    fullName: 'Utilizador',
    username: 'utilizador',
    email: 'utilizador@example.invalid',
    roles,
    mustChangePassword: false,
  };
}
