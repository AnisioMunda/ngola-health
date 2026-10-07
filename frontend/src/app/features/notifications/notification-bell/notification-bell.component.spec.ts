import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, Subject, throwError } from 'rxjs';
import { NotificationDto, NotificationService } from '../../../core/services/notification.service';
import { NotificationBellComponent } from './notification-bell.component';

describe('NotificationBellComponent', () => {
  let component: NotificationBellComponent;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: jasmine.SpyObj<Router>;
  const notification: NotificationDto = {
    id: 'notification-1',
    type: 'LAB_RESULT',
    priority: 'HIGH',
    title: 'Resultado de exame',
    message: 'Resultado disponível.',
    actionUrl: '/lab/request-1',
    referenceId: 'request-1',
    referenceType: 'LAB_REQUEST',
    read: false,
    readAt: null,
    createdAt: '2026-05-21T10:30:00Z',
  };

  beforeEach(() => {
    notification.read = false;
    notificationService = jasmine.createSpyObj<NotificationService>('NotificationService', [
      'startPolling',
      'getTopUnread',
      'markAsRead',
      'markAllAsRead',
    ]);
    Object.defineProperty(notificationService, 'unreadCount', { value: signal(2) });
    Object.defineProperty(notificationService, 'pollingError', { value: signal(false) });
    notificationService.getTopUnread.and.returnValue(of([notification]));
    notificationService.markAsRead.and.returnValue(of(void 0));
    notificationService.markAllAsRead.and.returnValue(of(void 0));
    router = jasmine.createSpyObj<Router>('Router', ['navigate', 'navigateByUrl']);

    TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        { provide: NotificationService, useValue: notificationService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(NotificationBellComponent).componentInstance;
    component.ngOnInit();
  });

  it('starts polling and loads the top unread notifications', () => {
    expect(notificationService.startPolling).toHaveBeenCalled();
    expect(component.notifications).toEqual([notification]);
    expect(component.loading).toBeFalse();
  });

  it('keeps a notification unread and reports an API error when marking fails', () => {
    notificationService.markAsRead.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Notificação não encontrada.' },
            status: 404,
          }),
      ),
    );
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.markAsRead(notification, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(notification.read).toBeFalse();
    expect(notificationService.unreadCount()).toBe(2);
    expect(component.error).toBe('Notificação não encontrada.');
  });

  it('navigates immediately but updates the unread count only after the API succeeds', () => {
    const response = new Subject<void>();
    notificationService.markAsRead.and.returnValue(response);

    component.open_(notification);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/lab/request-1');
    expect(notificationService.unreadCount()).toBe(2);

    response.next();
    response.complete();

    expect(notification.read).toBeTrue();
    expect(notificationService.unreadCount()).toBe(1);
  });
});
