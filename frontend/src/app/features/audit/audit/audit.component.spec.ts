import { of, throwError } from 'rxjs';
import { AuditService } from '../../../core/services/audit.service';
import { AuditComponent } from './audit.component';

describe('AuditComponent', () => {
  let component: AuditComponent;
  let auditService: jasmine.SpyObj<AuditService>;

  beforeEach(() => {
    auditService = jasmine.createSpyObj<AuditService>('AuditService', ['findAll', 'getStats']);
    auditService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0 }),
    );
    auditService.getStats.and.returnValue(
      of({ totalToday: 0, failedLoginsLast7Days: 0, topActiveUsers: [] }),
    );
    component = new AuditComponent(auditService);
  });

  it('validates the user UUID before making a filtered request', () => {
    component.userIdFilter = 'not-a-uuid';

    component.onFilterChange();

    expect(component.filterError).toContain('UUID válido');
    expect(auditService.findAll).not.toHaveBeenCalled();
  });

  it('rejects a date range whose start is after its end', () => {
    component.dateFrom = '2026-06-30';
    component.dateTo = '2026-06-01';

    component.onFilterChange();

    expect(component.filterError).toContain('data inicial');
    expect(auditService.findAll).not.toHaveBeenCalled();
  });

  it('applies supported filters, including hospitals and the selected user', () => {
    component.userIdFilter = '43b95f9e-629f-41f7-9501-3b7a8c638dec';
    component.actionFilter = 'LOGOUT';
    component.entityTypeFilter = 'HOSPITAL';
    component.dateFrom = '2026-06-01';
    component.dateTo = '2026-06-30';

    component.onFilterChange();

    expect(auditService.findAll).toHaveBeenCalledWith(
      '43b95f9e-629f-41f7-9501-3b7a8c638dec',
      'LOGOUT',
      'HOSPITAL',
      '2026-06-01T00:00:00Z',
      '2026-06-30T23:59:59Z',
      0,
    );
  });

  it('exposes statistics failures instead of silently hiding them', () => {
    auditService.getStats.and.returnValue(throwError(() => new Error('offline')));

    component.loadStats();

    expect(component.stats).toBeNull();
    expect(component.statsError).toContain('Não foi possível');
    expect(component.loadingStats).toBe(false);
  });
});
