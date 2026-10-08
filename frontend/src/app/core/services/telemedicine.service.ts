import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type SessionStatus =
  'SCHEDULED' | 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface SessionResponse {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  appointmentId: string;
  status: SessionStatus;
  statusLabel: string;
  roomToken: string;
  roomUrl: string | null;
  scheduledAt: string;
  startedAt: string;
  endedAt: string;
  durationMinutes: number;
  clinicalNotes: string;
  createdAt: string;
}

export interface TelemedicineStatsDto {
  totalScheduled: number;
  totalToday: number;
  inProgress: number;
  completedThisMonth: number;
  avgDurationMinutes: number;
  teamsConfigured: boolean;
}

export interface CreateSessionRequest {
  patientId: string;
  doctorId: string;
  scheduledAt: string;
  durationMinutes: number;
}

export interface DoctorOption {
  id: string;
  fullName: string;
}

export const SESSION_STATUS_COLORS: Record<SessionStatus, string> = {
  SCHEDULED: '#3b82f6',
  WAITING: '#f59e0b',
  IN_PROGRESS: '#16a34a',
  COMPLETED: '#6b7280',
  CANCELLED: '#dc2626',
  NO_SHOW: '#9ca3af',
};

@Injectable({ providedIn: 'root' })
export class TelemedicineService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/telemedicine`;

  getStats(): Observable<TelemedicineStatsDto> {
    return this.http.get<TelemedicineStatsDto>(`${this.apiUrl}/stats`);
  }
  getActiveSessions(): Observable<SessionResponse[]> {
    return this.http.get<SessionResponse[]>(`${this.apiUrl}/active`);
  }
  getMySessionsToday(): Observable<SessionResponse[]> {
    return this.http.get<SessionResponse[]>(`${this.apiUrl}/my-sessions`);
  }
  getTeamsDoctors(): Observable<DoctorOption[]> {
    return this.http.get<DoctorOption[]>(`${this.apiUrl}/doctors`);
  }
  create(req: CreateSessionRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(this.apiUrl, req);
  }
  joinSession(token: string): Observable<SessionResponse> {
    return this.http.patch<SessionResponse>(`${this.apiUrl}/room/${token}/join`, {});
  }
  patientWaiting(token: string): Observable<SessionResponse> {
    return this.http.patch<SessionResponse>(`${this.apiUrl}/room/${token}/waiting`, {});
  }
  endSession(id: string, clinicalNotes: string): Observable<SessionResponse> {
    return this.http.patch<SessionResponse>(`${this.apiUrl}/${id}/end`, { clinicalNotes });
  }
  cancel(id: string): Observable<SessionResponse> {
    return this.http.patch<SessionResponse>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
