import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthResponse, AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const refreshedSession: AuthResponse = {
    id: 'user-id',
    fullName: 'Utilizador de Teste',
    username: 'teste',
    email: 'teste@example.invalid',
    accessToken: 'access-token-novo',
    refreshToken: 'refresh-token-novo',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.setItem('refreshToken', 'refresh-token-antigo');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('partilha um refresh em curso e guarda os dois tokens rotacionados', () => {
    const responses: AuthResponse[] = [];
    service.refreshSession().subscribe((response) => responses.push(response));
    service.refreshSession().subscribe((response) => responses.push(response));

    const request = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-token-antigo' });
    request.flush(refreshedSession);

    expect(responses).toEqual([refreshedSession, refreshedSession]);
    expect(localStorage.getItem('accessToken')).toBe('access-token-novo');
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token-novo');
    expect(JSON.parse(localStorage.getItem('user') ?? '{}').username).toBe('teste');
  });
});
