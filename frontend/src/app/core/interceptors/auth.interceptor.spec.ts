import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthResponse, AuthService } from '../services/auth.service';

@Component({ standalone: true, template: '' })
class TestPageComponent {}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: Router;

  const refreshedSession: AuthResponse = {
    id: 'user-id',
    fullName: 'Utilizador de Teste',
    username: 'teste',
    email: 'teste@example.invalid',
    accessToken: 'access-token-novo',
    refreshToken: 'refresh-token-novo',
    roles: ['DOCTOR'],
    mustChangePassword: false,
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'patients/:id', component: TestPageComponent }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    localStorage.setItem('accessToken', 'access-token-expirado');
    localStorage.setItem('refreshToken', 'refresh-token-valido');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('anexa o token de acesso renovado e repete o pedido uma vez', async () => {
    const response = firstValueFrom(http.get<{ ok: boolean }>('/api/patients'));

    const firstRequest = httpMock.expectOne('/api/patients');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer access-token-expirado');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(refreshRequest.request.body).toEqual({ refreshToken: 'refresh-token-valido' });
    expect(refreshRequest.request.headers.has('Authorization')).toBe(false);
    refreshRequest.flush(refreshedSession);

    const retryRequest = httpMock.expectOne('/api/patients');
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer access-token-novo');
    retryRequest.flush({ ok: true });

    expect(await response).toEqual({ ok: true });
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token-novo');
  });

  it('termina a sessão e preserva a rota quando o refresh é recusado', async () => {
    await router.navigateByUrl('/patients/42');
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const response = firstValueFrom(http.get('/api/patients/42')).catch((error) => error);

    httpMock.expectOne('/api/patients/42').flush({}, { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect((await response).status).toBe(401);
    expect(authService.getAccessToken()).toBeNull();
    expect(authService.getRefreshToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/patients/42' },
      replaceUrl: true,
    });
  });
});
