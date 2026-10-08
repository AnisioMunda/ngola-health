import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AuditService } from './audit.service';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AuditService],
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends user, action, entity, date, and paging filters to the audit API', () => {
    service
      .findAll(
        '43b95f9e-629f-41f7-9501-3b7a8c638dec',
        'LOGOUT',
        'HOSPITAL',
        '2026-06-01T00:00:00Z',
        '2026-06-30T23:59:59Z',
        2,
        50,
      )
      .subscribe();

    const request = httpMock.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/audit/logs`,
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('userId')).toBe('43b95f9e-629f-41f7-9501-3b7a8c638dec');
    expect(request.request.params.get('action')).toBe('LOGOUT');
    expect(request.request.params.get('entityType')).toBe('HOSPITAL');
    expect(request.request.params.get('from')).toBe('2026-06-01T00:00:00Z');
    expect(request.request.params.get('to')).toBe('2026-06-30T23:59:59Z');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 2 });
  });
});
