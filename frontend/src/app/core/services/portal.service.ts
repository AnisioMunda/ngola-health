import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

export interface PortalLoginResponse {
  token: string;
  patientName: string;
  patientId: string;
  email: string;
}

export interface PortalDashboardDto {
  patientName: string;
  totalEpisodes: number;
  upcomingAppointments: number;
  pendingLabResults: number;
  pendingInvoices: number;
  totalDebt: number;
  recentEpisodes: PortalEpisodeDto[];
  upcomingAppointmentsList: PortalAppointmentDto[];
}

export interface PortalEpisodeDto {
  id: string;
  episodeType: string;
  status: string;
  statusLabel: string;
  doctorName: string;
  scheduledAt: string;
  completedAt: string;
  reason: string;
  diagnosis: string;
  hasLabResults: boolean;
  hasPrescriptions: boolean;
}

export interface PortalPrescriptionDto {
  id: string;
  prescriptionNumber: string;
  doctorName: string;
  prescriptionDate: string;
  expiryDate: string;
  status: string;
  statusLabel: string;
  diagnosis: string;
  items: PortalPrescriptionItemDto[];
}

export interface PortalPrescriptionItemDto {
  medicationName: string;
  dosage: string;
  route: string;
  quantityPrescribed: number;
  quantityDispensed: number;
  status: string;
}

export interface PortalInvoiceDto {
  id: string;
  invoiceNumber: string;
  issueDate: string;
  totalAmount: number;
  status: string;
  statusLabel: string;
  description: string;
}

export interface PortalAppointmentDto {
  id: string;
  doctorName: string;
  specialty: string;
  scheduledAt: string;
  status: string;
  reason: string;
}

const PORTAL_TOKEN_KEY = 'portal_token';
const PORTAL_USER_KEY  = 'portal_user';

@Injectable({ providedIn: 'root' })
export class PortalService {

  private http   = inject(HttpClient);
  private router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/portal`;

  private currentUser$ = new BehaviorSubject<PortalLoginResponse | null>(
    this.getStoredUser());

  // Auth
  register(req: any): Observable<PortalLoginResponse> {
    return this.http.post<PortalLoginResponse>(`${this.apiUrl}/register`, req)
      .pipe(tap(r => this.storeSession(r)));
  }

  login(email: string, password: string): Observable<PortalLoginResponse> {
    return this.http.post<PortalLoginResponse>(`${this.apiUrl}/login`, { email, password })
      .pipe(tap(r => this.storeSession(r)));
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
    } catch { return null; }
  }
}