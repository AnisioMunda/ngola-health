import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type DosageForm =
  | 'TABLET'
  | 'CAPSULE'
  | 'SYRUP'
  | 'INJECTION'
  | 'CREAM'
  | 'OINTMENT'
  | 'DROPS'
  | 'INHALER'
  | 'OTHER';

export interface MedicationResponse {
  id: string;
  name: string;
  genericName: string | null;
  dosageForm: DosageForm;
  strength: string | null;
  unit: string;
  requiresPrescription: boolean;
  minStockLevel: number | null;
  totalAvailable: number;
  active: boolean;
}

export interface CreateMedicationRequest {
  name: string;
  genericName: string | null;
  dosageForm: DosageForm;
  strength: string | null;
  unit: string;
  requiresPrescription: boolean;
  minStockLevel: number;
}

export interface StockBatchResponse {
  id: string;
  medicationId: string;
  medicationName: string;
  batchNumber: string;
  expiryDate: string;
  quantityReceived: number;
  quantityAvailable: number;
  unitCost: number | null;
  supplier: string | null;
  expired: boolean;
  expiringSoon: boolean;
  receivedAt: string;
}

export interface ReceiveStockRequest {
  medicationId: string;
  batchNumber: string;
  expiryDate: string;
  quantity: number;
  unitCost?: number;
  supplier?: string;
}

export interface DispenseRequest {
  medicationId: string;
  quantity: number;
  patientId?: string;
  episodeId?: string;
  reason?: string;
}

export interface DispenseResponse {
  medicationId: string;
  medicationName: string;
  quantityDispensed: number;
  remainingStock: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const DOSAGE_FORM_LABELS: Record<DosageForm, string> = {
  TABLET: 'Comprimido',
  CAPSULE: 'Cápsula',
  SYRUP: 'Xarope',
  INJECTION: 'Injectável',
  CREAM: 'Creme',
  OINTMENT: 'Pomada',
  DROPS: 'Gotas',
  INHALER: 'Inalador',
  OTHER: 'Outro',
};

export function pharmacyErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const body = error.error as Record<string, unknown>;
    if (typeof body['detail'] === 'string' && body['detail']) return body['detail'];
    if (typeof body['message'] === 'string' && body['message']) return body['message'];
  }
  return fallback;
}

@Injectable({ providedIn: 'root' })
export class PharmacyService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/pharmacy`;

  findAllMedications(search?: string, page = 0, size = 20): Observable<Page<MedicationResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search?.trim()) params = params.set('search', search.trim());
    return this.http.get<Page<MedicationResponse>>(`${this.apiUrl}/medications`, { params });
  }

  findMedication(id: string): Observable<MedicationResponse> {
    return this.http.get<MedicationResponse>(`${this.apiUrl}/medications/${id}`);
  }

  createMedication(request: CreateMedicationRequest): Observable<MedicationResponse> {
    return this.http.post<MedicationResponse>(`${this.apiUrl}/medications`, request);
  }

  findBatches(medicationId: string): Observable<StockBatchResponse[]> {
    return this.http.get<StockBatchResponse[]>(
      `${this.apiUrl}/medications/${medicationId}/batches`,
    );
  }

  receiveStock(request: ReceiveStockRequest): Observable<StockBatchResponse> {
    return this.http.post<StockBatchResponse>(`${this.apiUrl}/stock/receive`, request);
  }

  findExpiringSoon(days = 30): Observable<StockBatchResponse[]> {
    return this.http.get<StockBatchResponse[]>(`${this.apiUrl}/stock/expiring`, {
      params: new HttpParams().set('days', days),
    });
  }

  dispense(request: DispenseRequest): Observable<DispenseResponse> {
    return this.http.post<DispenseResponse>(`${this.apiUrl}/dispense`, request);
  }
}
