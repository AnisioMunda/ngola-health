import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { NotificationDto, NotificationService } from '../../../core/services/notification.service';
import { NotificationsComponent } from './notifications.component';

describe('NotificationsComponent', () => {
  let component: NotificationsComponent;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: jasmine.SpyObj<Router>;

  const notification: NotificationDto = {
    id: 'notification-1',
    type: 'SYSTEM',
    priority: 'MEDIUM',
    title: 'Aviso',
    message: 'Informação do sistema.',
    actionUrl: null,
    referenceId: null,
    referenceType: null,
    read: false,
    readAt: null,
    createdAt: '2026-05-21T10:30:00Z',
  };

  beforeEach(() => {
    notificationService = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'getAll',
      'markAsRead',
      'markAllAsRead',
    ]);
    Object.defineProperty(notificationService, 'unreadCount', { value: signal(1) });
    notificationService.getAll.and.returnValue(
      of({ content: [notification], totalElements: 1, totalPages: 1, number: 0 }),
    );
    notificationService.markAsRead.and.returnValue(of(void 0));
    notificationService.markAllAsRead.and.returnValue(of(void 0));
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      imports: [NotificationsComponent],
      providers: [
        { provide: NotificationService, useValue: notificationService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(NotificationsComponent).componentInstance;
    component.ngOnInit();
  });

  it('loads a paginated list of notifications', () => {
    expect(notificationService.getAll).toHaveBeenCalledWith(0);
    expect(component.notifications).toEqual([notification]);
    expect(component.totalElements).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('preserves unread state and displays an error when marking a notification fails', () => {
    notificationService.markAsRead.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não foi possível actualizar a notificação.' },
            status: 409,
          }),
      ),
    );

    component.markAsRead(notification);

    expect(notification.read).toBeFalse();
    expect(notificationService.unreadCount()).toBe(1);
    expect(component.error).toBe('Não foi possível actualizar a notificação.');
  });

  it('loads the next page only when one exists', () => {
    component.totalPages = 2;

    component.nextPage();

    expect(component.currentPage).toBe(1);
    expect(notificationService.getAll).toHaveBeenCalledWith(1);
  });
});
