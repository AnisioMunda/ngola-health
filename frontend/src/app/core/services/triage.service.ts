import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TriagePriority = 'RED' | 'ORANGE' | 'YELLOW' | 'GREEN' | 'BLUE';
export type TriageStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'TRANSFERRED' | 'LEFT';

export interface TriageResponse {
  id: string;
  queueNumber: number;
  patientId: string;
  patientName: string;
  patientAge: number;
  patientGender: string;
  priority: TriagePriority;
  priorityLabel: string;
  priorityColor: string;
  chiefComplaint: string;
  bloodPressure: string;
  heartRate: number;
  temperature: number;
  oxygenSaturation: number;
  respiratoryRate: number;
  weightKg: number;
  painScale: number;
  triageNotes: string;
  status: TriageStatus;
  statusLabel: string;
  triagedByName: string;
  episodeId: string;
  triagedAt: string;
  attendedAt: string;
  completedAt: string;
  waitingMinutes: number;
  overdue: boolean;
}

export interface TriageStatsDto {
  totalWaiting: number;
  totalToday: number;
  inProgress: number;
  completedToday: number;
  waitingRed: number;
  waitingOrange: number;
  waitingYellow: number;
  waitingGreen: number;
  waitingBlue: number;
  avgWaitingMinutes: number;
}

export const PRIORITY_LABELS: Record<TriagePriority, string> = {
  RED: 'Imediato',
  ORANGE: 'Muito Urgente',
  YELLOW: 'Urgente',
  GREEN: 'Pouco Urgente',
  BLUE: 'Não Urgente',
};

export const PRIORITY_COLORS: Record<TriagePriority, string> = {
  RED: '#dc2626',
  ORANGE: '#ea580c',
  YELLOW: '#ca8a04',
  GREEN: '#16a34a',
  BLUE: '#2563eb',
};

export const PRIORITY_MAX_WAIT: Record<TriagePriority, number> = {
  RED: 0,
  ORANGE: 10,
  YELLOW: 60,
  GREEN: 120,
  BLUE: 240,
};

export const STATUS_LABELS: Record<TriageStatus, string> = {
  WAITING: 'Em Espera',
  IN_PROGRESS: 'Em Atendimento',
  COMPLETED: 'Concluído',
  TRANSFERRED: 'Transferido',
  LEFT: 'Saiu',
};

@Injectable({ providedIn: 'root' })
export class TriageService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/triage`;

  getStats(): Observable<TriageStatsDto> {
    return this.http.get<TriageStatsDto>(`${this.apiUrl}/stats`);
  }

  getActiveQueue(): Observable<TriageResponse[]> {
    return this.http.get<TriageResponse[]>(`${this.apiUrl}/queue`);
  }

  getHistory(date?: string): Observable<TriageResponse[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<TriageResponse[]>(`${this.apiUrl}/history`, { params });
  }

  findById(id: string): Observable<TriageResponse> {
    return this.http.get<TriageResponse>(`${this.apiUrl}/${id}`);
  }

  create(req: any): Observable<TriageResponse> {
    return this.http.post<TriageResponse>(this.apiUrl, req);
  }

  updatePriority(
    id: string,
    priority: TriagePriority,
    reason?: string,
  ): Observable<TriageResponse> {
    return this.http.patch<TriageResponse>(`${this.apiUrl}/${id}/priority`, { priority, reason });
  }

  callNext(id: string): Observable<TriageResponse> {
    return this.http.patch<TriageResponse>(`${this.apiUrl}/${id}/call`, {});
  }

  complete(id: string): Observable<TriageResponse> {
    return this.http.patch<TriageResponse>(`${this.apiUrl}/${id}/complete`, {});
  }

  markAsLeft(id: string): Observable<TriageResponse> {
    return this.http.patch<TriageResponse>(`${this.apiUrl}/${id}/left`, {});
  }
}
