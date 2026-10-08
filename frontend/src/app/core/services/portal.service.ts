import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import type {
  PortalDashboardDto,
  PortalEpisodeDto,
  PortalInvoiceDto,
  PortalLabResultDto,
  PortalLoginRequest,
  PortalLoginResponse,
  PortalPrescriptionDto,
  PortalRegistrationResponse,
  PortalRegisterRequest,
} from '../api/generated/portal-api';

export type {
  PortalAppointmentDto,
  PortalDashboardDto,
  PortalEpisodeDto,
  PortalInvoiceDto,
  PortalLabResultDto,
  PortalLoginResponse,
  PortalPrescriptionDto,
  PortalPrescriptionItemDto,
  PortalRegistrationResponse,
} from '../api/generated/portal-api';

const PORTAL_TOKEN_KEY = 'portal_token';
const PORTAL_USER_KEY = 'portal_user';

@Injectable({ providedIn: 'root' })
export class PortalService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/portal`;

  private currentUser$ = new BehaviorSubject<PortalLoginResponse | null>(this.getStoredUser());

  // Auth
  register(req: PortalRegisterRequest): Observable<PortalRegistrationResponse> {
    return this.http.post<PortalRegistrationResponse>(`${this.apiUrl}/register`, req);
  }

  login(email: string, password: string): Observable<PortalLoginResponse> {
    return this.http
      .post<PortalLoginResponse>(`${this.apiUrl}/login`, {
        email,
        password,
      } satisfies PortalLoginRequest)
      .pipe(tap((r) => this.storeSession(r)));
  }

  logout(): void {
    localStorage.removeItem(PORTAL_TOKEN_KEY);
    localStorage.removeItem(PORTAL_USER_KEY);
    this.currentUser$.next(null);
    this.router.navigate(['/portal/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(PORTAL_TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): PortalLoginResponse | null {
    return this.currentUser$.getValue();
  }

  // Data
  getDashboard(): Observable<PortalDashboardDto> {
    return this.http.get<PortalDashboardDto>(`${this.apiUrl}/dashboard`);
  }

  getEpisodes(): Observable<PortalEpisodeDto[]> {
    return this.http.get<PortalEpisodeDto[]>(`${this.apiUrl}/episodes`);
  }

  getLabResults(): Observable<PortalLabResultDto[]> {
    return this.http.get<PortalLabResultDto[]>(`${this.apiUrl}/lab-results`);
  }

  getPrescriptions(): Observable<PortalPrescriptionDto[]> {
    return this.http.get<PortalPrescriptionDto[]>(`${this.apiUrl}/prescriptions`);
  }

  getInvoices(): Observable<PortalInvoiceDto[]> {
    return this.http.get<PortalInvoiceDto[]>(`${this.apiUrl}/invoices`);
  }

  private storeSession(r: PortalLoginResponse): void {
    localStorage.setItem(PORTAL_TOKEN_KEY, r.token);
    localStorage.setItem(PORTAL_USER_KEY, JSON.stringify(r));
    this.currentUser$.next(r);
  }

  private getStoredUser(): PortalLoginResponse | null {
    try {
      const s = localStorage.getItem(PORTAL_USER_KEY);
      return s ? JSON.parse(s) : null;
    } catch {
      return null;
    }
  }
}
