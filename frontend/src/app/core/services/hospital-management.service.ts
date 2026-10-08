import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type HospitalType = 'HOSPITAL' | 'CLINIC' | 'POLYCLINIC' | 'HEALTH_CENTER' | 'LABORATORY';

export interface HospitalResponse {
  id: string;
  name: string;
  code: string;
  type: HospitalType;
  province: string;
  municipality: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  taxId: string | null;
  active: boolean;
}

export interface HospitalRequest {
  name: string;
  code: string;
  type: HospitalType;
  province: string;
  municipality?: string;
  address?: string;
  phone?: string;
  email?: string;
  taxId?: string;
}

@Injectable({ providedIn: 'root' })
export class HospitalManagementService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/hospitals`;

  findAllForManagement(): Observable<HospitalResponse[]> {
    return this.http.get<HospitalResponse[]>(`${this.apiUrl}/management`);
  }

  findById(id: string): Observable<HospitalResponse> {
    return this.http.get<HospitalResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: HospitalRequest): Observable<HospitalResponse> {
    return this.http.post<HospitalResponse>(this.apiUrl, request);
  }

  update(id: string, request: HospitalRequest): Observable<HospitalResponse> {
    return this.http.put<HospitalResponse>(`${this.apiUrl}/${id}`, request);
  }

  activate(id: string): Observable<HospitalResponse> {
    return this.http.patch<HospitalResponse>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<HospitalResponse> {
    return this.http.patch<HospitalResponse>(`${this.apiUrl}/${id}/deactivate`, {});
  }
}
