// ============================================================
// equipment.service.ts
// ============================================================
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type EquipmentStatus = 'ACTIVE' | 'MAINTENANCE' | 'REPAIR' | 'RETIRED' | 'RESERVED';
export type EquipmentCategory =
  'DIAGNOSTIC' | 'THERAPEUTIC' | 'SURGICAL' | 'MONITORING' | 'MOBILITY' | 'IT' | 'OTHER';
export type MaintenanceType = 'PREVENTIVE' | 'CORRECTIVE' | 'CALIBRATION' | 'INSPECTION';

export interface MaintenanceResponse {
  id: string;
  type: MaintenanceType;
  typeLabel: string;
  performedByName: string;
  performedAt: string;
  description: string;
  cost: number;
  nextMaintenanceDate: string;
  partsReplaced: string;
  result: string;
}

export interface EquipmentResponse {
  id: string;
  name: string;
  code: string;
  brand: string;
  model: string;
  serialNumber: string;
  category: EquipmentCategory;
  categoryLabel: string;
  location: string;
  wardName: string;
  status: EquipmentStatus;
  statusLabel: string;
  purchaseDate: string;
  purchasePrice: number;
  warrantyExpiry: string;
  warrantyExpired: boolean;
  nextMaintenanceDate: string;
  maintenanceDue: boolean;
  nextCalibrationDate: string;
  maintenanceIntervalDays: number;
  notes: string;
  maintenanceHistory: MaintenanceResponse[];
  createdAt: string;
}

export interface EquipmentStatsDto {
  totalEquipment: number;
  activeEquipment: number;
  inMaintenance: number;
  maintenanceDue: number;
  calibrationDue: number;
  warrantyExpired: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const STATUS_COLORS: Record<EquipmentStatus, string> = {
  ACTIVE: '#16a34a',
  MAINTENANCE: '#f59e0b',
  REPAIR: '#dc2626',
  RETIRED: '#9ca3af',
  RESERVED: '#3b82f6',
};

export const CATEGORY_LABELS: Record<EquipmentCategory, string> = {
  DIAGNOSTIC: 'Diagnóstico',
  THERAPEUTIC: 'Terapêutico',
  SURGICAL: 'Cirúrgico',
  MONITORING: 'Monitorização',
  MOBILITY: 'Mobilidade',
  IT: 'Informático',
  OTHER: 'Outro',
};

@Injectable({ providedIn: 'root' })
export class EquipmentService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/equipment`;

  getStats(): Observable<EquipmentStatsDto> {
    return this.http.get<EquipmentStatsDto>(`${this.apiUrl}/stats`);
  }

  findAll(q = '', page = 0): Observable<Page<EquipmentResponse>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (q) params = params.set('q', q);
    return this.http.get<Page<EquipmentResponse>>(this.apiUrl, { params });
  }

  findById(id: string): Observable<EquipmentResponse> {
    return this.http.get<EquipmentResponse>(`${this.apiUrl}/${id}`);
  }

  findMaintenanceDue(): Observable<EquipmentResponse[]> {
    return this.http.get<EquipmentResponse[]>(`${this.apiUrl}/maintenance-due`);
  }

  create(req: any): Observable<EquipmentResponse> {
    return this.http.post<EquipmentResponse>(this.apiUrl, req);
  }

  updateStatus(id: string, status: EquipmentStatus, notes?: string): Observable<EquipmentResponse> {
    return this.http.patch<EquipmentResponse>(`${this.apiUrl}/${id}/status`, { status, notes });
  }

  addMaintenance(id: string, req: any): Observable<EquipmentResponse> {
    return this.http.post<EquipmentResponse>(`${this.apiUrl}/${id}/maintenance`, req);
  }
}
