import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type AuditAction =
  'CREATE' | 'READ' | 'UPDATE' | 'DELETE' |
  'LOGIN' | 'LOGOUT' | 'LOGIN_FAILED' |
  'EXPORT' | 'PRINT' | 'APPROVE' | 'REJECT';

export type EntityType =
  'PATIENT' | 'EPISODE' | 'LAB_REQUEST' | 'MEDICATION' |
  'INVOICE' | 'APPOINTMENT' | 'ADMISSION' | 'USER' |
  'NOTIFICATION' | 'REPORT' | 'WARD' | 'BED' | 'SYSTEM';

export type AuditResult = 'SUCCESS' | 'FAILURE' | 'UNAUTHORIZED';

export interface AuditLogDto {
  id: string;
  action: AuditAction;
  entityType: EntityType;
  entityId: string;
  description: string;
  username: string;
  userFullName: string;
  ipAddress: string;
  httpMethod: string;
  requestUrl: string;
  result: AuditResult;
  errorMessage: string;
  createdAt: string;
}

export interface AuditStatsDto {
  totalToday: number;
  failedLoginsLast7Days: number;
  topActiveUsers: { username: string; fullName: string; total: number }[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const ACTION_LABELS: Record<AuditAction, string> = {
  CREATE:       'Criação',
  READ:         'Leitura',
  UPDATE:       'Actualização',
  DELETE:       'Eliminação',
  LOGIN:        'Login',
  LOGOUT:       'Logout',
  LOGIN_FAILED: 'Login Falhado',
  EXPORT:       'Exportação',
  PRINT:        'Impressão',
  APPROVE:      'Aprovação',
  REJECT:       'Rejeição'
};

export const ENTITY_LABELS: Record<EntityType, string> = {
  PATIENT:      'Paciente',
  EPISODE:      'Episódio',
  LAB_REQUEST:  'Lab',
  MEDICATION:   'Medicamento',
  INVOICE:      'Factura',
  APPOINTMENT:  'Agendamento',
  ADMISSION:    'Internamento',
  USER:         'Utilizador',
  NOTIFICATION: 'Notificação',
  REPORT:       'Relatório',
  WARD:         'Enfermaria',
  BED:          'Cama',
  SYSTEM:       'Sistema'
};

export const RESULT_COLORS: Record<AuditResult, string> = {
  SUCCESS:      '#16a34a',
  FAILURE:      '#dc2626',
  UNAUTHORIZED: '#f59e0b'
};

export const ACTION_ICONS: Record<AuditAction, string> = {
  CREATE:       '➕',
  READ:         '👁',
  UPDATE:       '✏️',
  DELETE:       '🗑',
  LOGIN:        '🔐',
  LOGOUT:       '🚪',
  LOGIN_FAILED: '⚠️',
  EXPORT:       '📤',
  PRINT:        '🖨️',
  APPROVE:      '✅',
  REJECT:       '❌'
};

@Injectable({ providedIn: 'root' })
export class AuditService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/audit`;

  findAll(
    userId?: string,
    action?: AuditAction,
    entityType?: EntityType,
    from?: string,
    to?: string,
    page = 0,
    size = 30
  ): Observable<Page<AuditLogDto>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (userId)     params = params.set('userId', userId);
    if (action)     params = params.set('action', action);
    if (entityType) params = params.set('entityType', entityType);
    if (from)       params = params.set('from', from);
    if (to)         params = params.set('to', to);
    return this.http.get<Page<AuditLogDto>>(`${this.apiUrl}/logs`, { params });
  }

  findByEntity(type: EntityType, id: string): Observable<AuditLogDto[]> {
    return this.http.get<AuditLogDto[]>(`${this.apiUrl}/logs/entity/${type}/${id}`);
  }

  getStats(): Observable<AuditStatsDto> {
    return this.http.get<AuditStatsDto>(`${this.apiUrl}/stats`);
  }
}