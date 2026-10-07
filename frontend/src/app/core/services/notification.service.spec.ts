import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), NotificationService],
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests a page with bounded API paging parameters', () => {
    service.getAll(2, 50).subscribe();

    const request = httpMock.expectOne(`${environment.apiUrl}/notifications?page=2&size=50`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 2 });
  });

  it('continues polling after errors and starts only one polling stream', fakeAsync(() => {
    let calls = 0;
    spyOn(service, 'getUnreadCount').and.callFake(() => {
      calls += 1;
      return calls === 1
        ? throwError(() => new HttpErrorResponse({ status: 503 }))
        : of({ count: calls });
    });

    service.startPolling();
    service.startPolling();

    expect(calls).toBe(1);
    expect(service.pollingError()).toBeTrue();

    tick(60_000);

    expect(calls).toBe(2);
    expect(service.unreadCount()).toBe(2);
    expect(service.pollingError()).toBeFalse();

    tick(60_000);

    expect(calls).toBe(3);
    expect(service.unreadCount()).toBe(3);
    discardPeriodicTasks();
  }));
});
