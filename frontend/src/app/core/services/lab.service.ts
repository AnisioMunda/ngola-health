import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type RequestStatus = 'PENDING' | 'COLLECTED' | 'IN_ANALYSIS' | 'COMPLETED' | 'CANCELLED';
export type Priority = 'NORMAL' | 'URGENT' | 'STAT';
export type TestCategory = 'HEMATOLOGY' | 'BIOCHEMISTRY' | 'MICROBIOLOGY' | 'IMMUNOLOGY' | 'URINE' | 'IMAGING' | 'CARDIOLOGY' | 'OTHER';

export interface LabTestResponse {
  id: string;
  code: string;
  name: string;
  category: TestCategory;
  sampleType: string;
  turnaroundHours: number;
  price: number;
  referenceValues: string;
  active: boolean;
}

export interface LabRequestItemResponse {
  id: string;
  labTestId: string;
  testCode: string;
  testName: string;
  resultValue: string;
  resultUnit: string;
  referenceRange: string;
  abnormal: boolean;
  resultNotes: string;
  resultedAt: string;
}

export interface LabRequestResponse {
  id: string;
  patientId: string;
  patientName: string;
  episodeId: string;
  requestedById: string;
  requestedByName: string;
  status: RequestStatus;
  priority: Priority;
  clinicalNotes: string;
  collectedAt: string;
  completedAt: string;
  createdAt: string;
  items: LabRequestItemResponse[];
}

export interface CreateLabRequestRequest {
  patientId: string;
  episodeId?: string;
  requestedById?: string;
  priority?: Priority;
  clinicalNotes?: string;
  labTestIds: string[];
}

export interface SubmitResultRequest {
  resultValue: string;
  resultUnit?: string;
  referenceRange?: string;
  abnormal: boolean;
  resultNotes?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const STATUS_LABELS: Record<RequestStatus, string> = {
  PENDING:     'Pending',
  COLLECTED:   'Collected',
  IN_ANALYSIS: 'In Analysis',
  COMPLETED:   'Completed',
  CANCELLED:   'Cancelled'
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  NORMAL: 'Normal',
  URGENT: 'Urgent',
  STAT:   'STAT'
};

@Injectable({ providedIn: 'root' })
export class LabService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/lab`;

  findAllTests(): Observable<LabTestResponse[]> {
    return this.http.get<LabTestResponse[]>(`${this.apiUrl}/tests`);
  }

  findAllRequests(
    patientId?: string,
    status?: RequestStatus,
    page = 0,
    size = 20
  ): Observable<Page<LabRequestResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (patientId) params = params.set('patientId', patientId);
    if (status) params = params.set('status', status);
    return this.http.get<Page<LabRequestResponse>>(`${this.apiUrl}/requests`, { params });
  }

  findRequestById(id: string): Observable<LabRequestResponse> {
    return this.http.get<LabRequestResponse>(`${this.apiUrl}/requests/${id}`);
  }

  createRequest(request: CreateLabRequestRequest): Observable<LabRequestResponse> {
    return this.http.post<LabRequestResponse>(`${this.apiUrl}/requests`, request);
  }

  collect(id: string): Observable<LabRequestResponse> {
    return this.http.patch<LabRequestResponse>(`${this.apiUrl}/requests/${id}/collect`, {});
  }

  startAnalysis(id: string): Observable<LabRequestResponse> {
    return this.http.patch<LabRequestResponse>(`${this.apiUrl}/requests/${id}/start-analysis`, {});
  }

  submitResult(requestId: string, itemId: string, result: SubmitResultRequest): Observable<LabRequestResponse> {
    return this.http.patch<LabRequestResponse>(
      `${this.apiUrl}/requests/${requestId}/items/${itemId}/result`, result);
  }

  cancel(id: string): Observable<LabRequestResponse> {
    return this.http.patch<LabRequestResponse>(`${this.apiUrl}/requests/${id}/cancel`, {});
  }
}