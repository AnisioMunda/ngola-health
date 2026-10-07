import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type NotificationType =
  | 'LOW_STOCK'
  | 'EXPIRING_STOCK'
  | 'LAB_RESULT'
  | 'APPOINTMENT'
  | 'APPOINTMENT_CANCELLED'
  | 'INVOICE_OVERDUE'
  | 'SYSTEM';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface NotificationDto {
  id: string;
  type: NotificationType;
  priority: Priority;
  title: string;
  message: string;
  actionUrl: string;
  referenceId: string;
  referenceType: string;
  read: boolean;
  readAt: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const TYPE_ICONS: Record<NotificationType, string> = {
  LOW_STOCK: '💊',
  EXPIRING_STOCK: '⏱',
  LAB_RESULT: '🔬',
  APPOINTMENT: '📅',
  APPOINTMENT_CANCELLED: '❌',
  INVOICE_OVERDUE: '💶',
  SYSTEM: '🔔',
};

export const PRIORITY_COLORS: Record<Priority, string> = {
  LOW: '#6b7280',
  MEDIUM: '#3b82f6',
  HIGH: '#f59e0b',
  CRITICAL: '#ef4444',
};

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notifications`;

  // Signal reactivo para o contador de não lidas
  unreadCount = signal<number>(0);

  // Polling automático a cada 60 segundos
  startPolling(): void {
    interval(60_000)
      .pipe(
        startWith(0),
        switchMap(() => this.getUnreadCount()),
      )
      .subscribe({
        next: (res) => this.unreadCount.set(res.count),
        error: () => {},
      });
  }

  getAll(page = 0, size = 20): Observable<Page<NotificationDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<NotificationDto>>(this.apiUrl, { params });
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`);
  }

  getTopUnread(): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(`${this.apiUrl}/top-unread`);
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {});
  }
}
