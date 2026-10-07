import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, interval } from 'rxjs';
import { catchError, startWith, switchMap, tap } from 'rxjs/operators';
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
  actionUrl: string | null;
  referenceId: string | null;
  referenceType: string | null;
  read: boolean;
  readAt: string | null;
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

export function notificationErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const body = error.error as Record<string, unknown>;
    if (typeof body['detail'] === 'string' && body['detail']) return body['detail'];
    if (typeof body['message'] === 'string' && body['message']) return body['message'];
  }
  return fallback;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiUrl = `${environment.apiUrl}/notifications`;
  private pollingStarted = false;

  // Signal reactivo para o contador de não lidas
  unreadCount = signal<number>(0);
  pollingError = signal(false);

  // Polling automático a cada 60 segundos
  startPolling(): void {
    if (this.pollingStarted) return;
    this.pollingStarted = true;

    interval(60_000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.getUnreadCount().pipe(
            tap((response) => {
              this.unreadCount.set(response.count);
              this.pollingError.set(false);
            }),
            catchError(() => {
              this.pollingError.set(true);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
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
