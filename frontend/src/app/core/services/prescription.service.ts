import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type PrescriptionStatus =
  'ACTIVE' | 'PARTIALLY_DISPENSED' | 'DISPENSED' | 'CANCELLED' | 'EXPIRED';

export type ItemStatus = 'PENDING' | 'DISPENSED' | 'PARTIAL' | 'CANCELLED';

export interface PrescriptionItemResponse {
  id: string;
  medicationId: string;
  medicationName: string;
  medicationUnit: string;
  quantityPrescribed: number;
  quantityDispensed: number;
  remainingQuantity: number;
  dosage: string;
  frequencyHours: number | null;
  durationDays: number | null;
  route: string | null;
  instructions: string | null;
  status: ItemStatus;
  statusLabel: string;
  stockAvailable: number;
}

export interface DispensationResponse {
  id: string;
  prescriptionItemId: string;
  medicationName: string;
  batchNumber: string;
  dispensedByName: string;
  quantityDispensed: number;
  dispensedAt: string;
  notes: string | null;
}

export interface PrescriptionResponse {
  id: string;
  prescriptionNumber: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  episodeId: string | null;
  admissionId: string | null;
  status: PrescriptionStatus;
  statusLabel: string;
  prescriptionDate: string;
  expiryDate: string;
  expired: boolean;
  diagnosis: string | null;
  notes: string | null;
  cancelledReason: string | null;
  items: PrescriptionItemResponse[];
  dispensations: DispensationResponse[];
  createdAt: string | null;
}

export interface CreatePrescriptionItemRequest {
  medicationId: string;
  quantityPrescribed: number;
  dosage: string;
  frequencyHours?: number | null;
  durationDays?: number | null;
  route?: string | null;
  instructions?: string | null;
}

export interface CreatePrescriptionRequest {
  patientId: string;
  episodeId?: string | null;
  admissionId?: string | null;
  diagnosis?: string | null;
  notes?: string | null;
  validityDays: number;
  items: CreatePrescriptionItemRequest[];
}

export interface DispensePrescriptionRequest {
  prescriptionItemId: string;
  quantityToDispense: number;
  notes?: string | null;
}

export interface PrescriptionStatsDto {
  totalActive: number;
  totalToday: number;
  pendingDispense: number;
  expiringSoon: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const PRESCRIPTION_STATUS_LABELS: Record<PrescriptionStatus, string> = {
  ACTIVE: 'Activa',
  PARTIALLY_DISPENSED: 'Parcialmente Dispensada',
  DISPENSED: 'Dispensada',
  CANCELLED: 'Cancelada',
  EXPIRED: 'Expirada',
};

export const ITEM_STATUS_LABELS: Record<ItemStatus, string> = {
  PENDING: 'Pendente',
  DISPENSED: 'Dispensado',
  PARTIAL: 'Parcial',
  CANCELLED: 'Cancelado',
};

export const STATUS_COLORS: Record<PrescriptionStatus, string> = {
  ACTIVE: '#3b82f6',
  PARTIALLY_DISPENSED: '#f59e0b',
  DISPENSED: '#16a34a',
  CANCELLED: '#dc2626',
  EXPIRED: '#9ca3af',
};

export const ROUTES = [
  'Oral',
  'Intravenosa',
  'Intramuscular',
  'Subcutânea',
  'Tópica',
  'Inalatória',
  'Sublingual',
  'Rectal',
  'Oftálmica',
];

export function prescriptionErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const body = error.error as Record<string, unknown>;
    if (typeof body['detail'] === 'string' && body['detail']) return body['detail'];
    if (typeof body['message'] === 'string' && body['message']) return body['message'];
  }
  return fallback;
}

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/prescriptions`;

  getStats(): Observable<PrescriptionStatsDto> {
    return this.http.get<PrescriptionStatsDto>(`${this.apiUrl}/stats`);
  }

  findAll(from?: string, to?: string, page = 0): Observable<Page<PrescriptionResponse>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<Page<PrescriptionResponse>>(this.apiUrl, { params });
  }

  findById(id: string): Observable<PrescriptionResponse> {
    return this.http.get<PrescriptionResponse>(`${this.apiUrl}/${id}`);
  }

  findByPatient(patientId: string, page = 0): Observable<Page<PrescriptionResponse>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<PrescriptionResponse>>(`${this.apiUrl}/patient/${patientId}`, {
      params,
    });
  }

  findByEpisode(episodeId: string): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.apiUrl}/episode/${episodeId}`);
  }

  findByAdmission(admissionId: string): Observable<PrescriptionResponse[]> {
    return this.http.get<PrescriptionResponse[]>(`${this.apiUrl}/admission/${admissionId}`);
  }

  create(req: CreatePrescriptionRequest): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(this.apiUrl, req);
  }

  dispense(id: string, req: DispensePrescriptionRequest): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(`${this.apiUrl}/${id}/dispense`, req);
  }

  cancel(id: string, reason: string): Observable<PrescriptionResponse> {
    return this.http.patch<PrescriptionResponse>(`${this.apiUrl}/${id}/cancel`, { reason });
  }
}
