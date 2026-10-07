import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type WardType =
  | 'GENERAL'
  | 'PEDIATRIC'
  | 'MATERNITY'
  | 'ICU'
  | 'SURGICAL'
  | 'CARDIOLOGY'
  | 'ONCOLOGY'
  | 'EMERGENCY'
  | 'ISOLATION';

export type BedStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE' | 'RESERVED';
export type EditableBedStatus = Exclude<BedStatus, 'OCCUPIED'>;
export type BedType = 'STANDARD' | 'PRIVATE' | 'SEMI_PRIVATE' | 'ICU' | 'ISOLATION';
export type AdmissionStatus = 'ACTIVE' | 'DISCHARGED' | 'TRANSFERRED' | 'DECEASED';
export type DischargeCondition =
  'IMPROVED' | 'STABLE' | 'CRITICAL' | 'DECEASED' | 'AGAINST_MEDICAL_ADVICE';

export interface WardResponse {
  id: string;
  name: string;
  code: string;
  type: WardType;
  typeLabel: string;
  floor: string | null;
  totalBeds: number;
  availableBeds: number;
  occupiedBeds: number;
  responsibleDoctorName: string | null;
  notes: string | null;
  active: boolean;
  beds?: BedResponse[];
}

export interface CreateWardRequest {
  name: string;
  code: string;
  type: WardType;
  floor: string | null;
  notes: string | null;
  responsibleDoctorId: string | null;
}

export interface CreateBedRequest {
  wardId: string;
  bedNumber: string;
  type: BedType;
  notes: string | null;
}

export interface BedResponse {
  id: string;
  bedNumber: string;
  status: BedStatus;
  statusLabel: string;
  type: BedType;
  wardName: string;
  wardId: string;
  notes: string | null;
  patientName: string | null;
  admissionId: string | null;
  admissionDate: string | null;
}

export interface WardMapResponse {
  wardId: string;
  wardName: string;
  wardType: string;
  totalBeds: number;
  availableBeds: number;
  occupiedBeds: number;
  maintenanceBeds: number;
  beds: BedResponse[];
}

export interface TransferResponse {
  id: string;
  fromBedNumber: string;
  fromWardName: string;
  toBedNumber: string;
  toWardName: string;
  reason: string;
  transferredByName: string;
  transferredAt: string;
}

export interface AdmissionResponse {
  id: string;
  patientId: string;
  patientName: string;
  patientPhone: string;
  bedId: string;
  bedNumber: string;
  wardId: string;
  wardName: string;
  wardType: string;
  doctorId: string;
  doctorName: string;
  status: AdmissionStatus;
  statusLabel: string;
  admissionDate: string;
  expectedDischargeDate: string;
  dischargeDate: string;
  admissionReason: string;
  diagnosis: string;
  dischargeNotes: string;
  dischargeCondition: DischargeCondition;
  daysAdmitted: number;
  admittedByName: string;
  dischargedByName: string;
  createdAt: string;
  transfers: TransferResponse[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const WARD_TYPE_LABELS: Record<WardType, string> = {
  GENERAL: 'Medicina Geral',
  PEDIATRIC: 'Pediatria',
  MATERNITY: 'Maternidade',
  ICU: 'UCI',
  SURGICAL: 'Cirurgia',
  CARDIOLOGY: 'Cardiologia',
  ONCOLOGY: 'Oncologia',
  EMERGENCY: 'Urgência',
  ISOLATION: 'Isolamento',
};

export const BED_STATUS_LABELS: Record<BedStatus, string> = {
  AVAILABLE: 'Disponível',
  OCCUPIED: 'Ocupada',
  MAINTENANCE: 'Manutenção',
  RESERVED: 'Reservada',
};

export const ADMISSION_STATUS_LABELS: Record<AdmissionStatus, string> = {
  ACTIVE: 'Internado',
  DISCHARGED: 'Alta',
  TRANSFERRED: 'Transferido',
  DECEASED: 'Óbito',
};

export const DISCHARGE_CONDITION_LABELS: Record<DischargeCondition, string> = {
  IMPROVED: 'Melhorado',
  STABLE: 'Estável',
  CRITICAL: 'Crítico',
  DECEASED: 'Óbito',
  AGAINST_MEDICAL_ADVICE: 'Contra Indicação Médica',
};

@Injectable({ providedIn: 'root' })
export class InpatientService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/inpatient`;

  // Enfermarias
  findAllWards(): Observable<WardResponse[]> {
    return this.http.get<WardResponse[]>(`${this.apiUrl}/wards`);
  }

  createWard(request: CreateWardRequest): Observable<WardResponse> {
    return this.http.post<WardResponse>(`${this.apiUrl}/wards`, request);
  }

  getWardMap(wardId: string): Observable<WardMapResponse> {
    return this.http.get<WardMapResponse>(`${this.apiUrl}/wards/${wardId}/map`);
  }

  // Camas
  findBedsByWard(wardId: string): Observable<BedResponse[]> {
    return this.http.get<BedResponse[]>(`${this.apiUrl}/wards/${wardId}/beds`);
  }

  createBed(request: CreateBedRequest): Observable<BedResponse> {
    return this.http.post<BedResponse>(`${this.apiUrl}/beds`, request);
  }

  updateBedStatus(
    bedId: string,
    status: EditableBedStatus,
    notes?: string,
  ): Observable<BedResponse> {
    return this.http.patch<BedResponse>(`${this.apiUrl}/beds/${bedId}/status`, { status, notes });
  }

  // Internamentos
  findAll(
    status?: AdmissionStatus,
    wardId?: string,
    page = 0,
    size = 20,
  ): Observable<Page<AdmissionResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (wardId) params = params.set('wardId', wardId);
    return this.http.get<Page<AdmissionResponse>>(`${this.apiUrl}/admissions`, { params });
  }

  findActive(): Observable<AdmissionResponse[]> {
    return this.http.get<AdmissionResponse[]>(`${this.apiUrl}/admissions/active`);
  }

  findById(id: string): Observable<AdmissionResponse> {
    return this.http.get<AdmissionResponse>(`${this.apiUrl}/admissions/${id}`);
  }

  admit(req: any): Observable<AdmissionResponse> {
    return this.http.post<AdmissionResponse>(`${this.apiUrl}/admissions`, req);
  }

  discharge(id: string, req: any): Observable<AdmissionResponse> {
    return this.http.patch<AdmissionResponse>(`${this.apiUrl}/admissions/${id}/discharge`, req);
  }

  transfer(id: string, req: any): Observable<AdmissionResponse> {
    return this.http.patch<AdmissionResponse>(`${this.apiUrl}/admissions/${id}/transfer`, req);
  }
}
