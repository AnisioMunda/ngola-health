import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PatientResponse {
  id: string;
  fullName: string;
  birthDate: string;
  age: number;
  gender: 'MALE' | 'FEMALE';
  nationalId: string;
  healthCardNumber: string;
  phone: string;
  email: string;
  address: string;
  province: string;
  municipality: string;
  emergencyContactName: string;
  emergencyContactPhone: string;
  emergencyContactRelationship: string;
  bloodType: string;
  allergies: string;
  chronicConditions: string;
  notes: string;
  active: boolean;
  createdAt: string;
}

export interface CreatePatientRequest {
  fullName: string;
  birthDate: string;
  gender: 'MALE' | 'FEMALE';
  nationalId?: string;
  healthCardNumber?: string;
  phone?: string;
  email?: string;
  address?: string;
  province?: string;
  municipality?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  bloodType?: string;
  allergies?: string;
  chronicConditions?: string;
  notes?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const ANGOLA_PROVINCES = [
  'Bengo', 'Benguela', 'Bié', 'Cabinda', 'Cuando Cubango',
  'Cuanza Norte', 'Cuanza Sul', 'Cunene', 'Huambo', 'Huíla',
  'Luanda', 'Lunda Norte', 'Lunda Sul', 'Malanje', 'Moxico',
  'Namibe', 'Uíge', 'Zaire'
];

export const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

@Injectable({ providedIn: 'root' })
export class PatientService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/patients`;

  findAll(search?: string, page = 0, size = 20): Observable<Page<PatientResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'fullName');
    if (search?.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Page<PatientResponse>>(this.apiUrl, { params });
  }

  findById(id: string): Observable<PatientResponse> {
    return this.http.get<PatientResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreatePatientRequest): Observable<PatientResponse> {
    return this.http.post<PatientResponse>(this.apiUrl, request);
  }

  update(id: string, request: CreatePatientRequest): Observable<PatientResponse> {
    return this.http.put<PatientResponse>(`${this.apiUrl}/${id}`, request);
  }

  deactivate(id: string): Observable<PatientResponse> {
    return this.http.delete<PatientResponse>(`${this.apiUrl}/${id}`);
  }
}