import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type EpisodeType = 'EMERGENCY' | 'OUTPATIENT' | 'INPATIENT' | 'OUTPATIENT_SURGERY' | 'EXAM';
export type EpisodeStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface EpisodeResponse {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  episodeType: EpisodeType;
  status: EpisodeStatus;
  scheduledAt: string;
  startedAt: string;
  completedAt: string;
  reason: string;
  symptoms: string;
  diagnosis: string;
  prescription: string;
  notes: string;
  bloodPressure: string;
  heartRate: number;
  temperature: number;
  weightKg: number;
  createdAt: string;
}

export interface CreateEpisodeRequest {
  patientId: string;
  doctorId?: string;
  episodeType: EpisodeType;
  scheduledAt?: string;
  reason?: string;
  symptoms?: string;
  diagnosis?: string;
  prescription?: string;
  notes?: string;
  bloodPressure?: string;
  heartRate?: number;
  temperature?: number;
  weightKg?: number;
}

export interface UpdateEpisodeRequest {
  doctorId?: string;
  reason?: string;
  symptoms?: string;
  diagnosis?: string;
  prescription?: string;
  notes?: string;
  bloodPressure?: string;
  heartRate?: number;
  temperature?: number;
  weightKg?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const EPISODE_TYPE_LABELS: Record<EpisodeType, string> = {
  EMERGENCY: 'Emergency',
  OUTPATIENT: 'Outpatient',
  INPATIENT: 'Inpatient',
  OUTPATIENT_SURGERY: 'Surgery',
  EXAM: 'Exam',
};

export const EPISODE_STATUS_LABELS: Record<EpisodeStatus, string> = {
  SCHEDULED: 'Scheduled',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

@Injectable({ providedIn: 'root' })
export class EpisodeService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/episodes`;

  findAll(
    patientId?: string,
    doctorId?: string,
    status?: EpisodeStatus,
    page = 0,
    size = 20,
  ): Observable<Page<EpisodeResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (patientId) params = params.set('patientId', patientId);
    if (doctorId) params = params.set('doctorId', doctorId);
    if (status) params = params.set('status', status);
    return this.http.get<Page<EpisodeResponse>>(this.apiUrl, { params });
  }

  findById(id: string): Observable<EpisodeResponse> {
    return this.http.get<EpisodeResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateEpisodeRequest): Observable<EpisodeResponse> {
    return this.http.post<EpisodeResponse>(this.apiUrl, request);
  }

  update(id: string, request: UpdateEpisodeRequest): Observable<EpisodeResponse> {
    return this.http.put<EpisodeResponse>(`${this.apiUrl}/${id}`, request);
  }

  start(id: string): Observable<EpisodeResponse> {
    return this.http.patch<EpisodeResponse>(`${this.apiUrl}/${id}/start`, {});
  }

  complete(id: string): Observable<EpisodeResponse> {
    return this.http.patch<EpisodeResponse>(`${this.apiUrl}/${id}/complete`, {});
  }

  cancel(id: string): Observable<EpisodeResponse> {
    return this.http.patch<EpisodeResponse>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
