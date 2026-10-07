import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserResponse {
  id: string;
  fullName: string;
  username: string;
  email: string;
  phone: string;
  especiality: string;
  professionalCard: string;
  registerStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  mustChangePassword: boolean;
  lastLogin: string;
  createdAt: string;
  roles: string[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateUserRequest {
  fullName: string;
  username: string;
  email: string;
  password: string;
  phone?: string;
  especiality?: string;
  professionalCard?: string;
  mustChangePassword: boolean;
  roleIds: string[];
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  phone?: string;
  especiality?: string;
  professionalCard?: string;
  roleIds?: string[];
}

export const ROLES = [
  { id: '00000000-0000-0000-0000-000000000001', name: 'ADMIN' },
  { id: '00000000-0000-0000-0000-000000000002', name: 'DOCTOR' },
  { id: '00000000-0000-0000-0000-000000000003', name: 'NURSE' },
  { id: '00000000-0000-0000-0000-000000000004', name: 'RECEPTIONIST' },
  { id: '00000000-0000-0000-0000-000000000005', name: 'PHARMACIST' },
  { id: '00000000-0000-0000-0000-000000000006', name: 'FINANCIAL' },
  { id: '00000000-0000-0000-0000-000000000007', name: 'MANAGER' },
  { id: '00000000-0000-0000-0000-000000000008', name: 'LAB_TECHNICIAN' },
];

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/users`;

  findAll(page = 0, size = 20): Observable<Page<UserResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'fullName');
    return this.http.get<Page<UserResponse>>(this.apiUrl, { params });
  }

  findById(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  update(id: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, request);
  }

  activate(id: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/activate`, {});
  }

  deactivate(id: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  suspend(id: string): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${id}/suspend`, {});
  }
}
