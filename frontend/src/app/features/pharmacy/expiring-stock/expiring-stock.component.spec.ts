import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PharmacyService, StockBatchResponse } from '../../../core/services/pharmacy.service';
import { ExpiringStockComponent } from './expiring-stock.component';

function todayInLuanda(): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Africa/Luanda',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

describe('ExpiringStockComponent', () => {
  let component: ExpiringStockComponent;
  let pharmacyService: jasmine.SpyObj<PharmacyService>;
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;

  const batch: StockBatchResponse = {
    id: 'batch-1',
    medicationId: 'med-1',
    medicationName: 'Amoxicilina',
    batchNumber: 'L-001',
    expiryDate: '2030-05-01',
    quantityReceived: 20,
    quantityAvailable: 12,
    unitCost: null,
    supplier: null,
    expired: false,
    expiringSoon: true,
    receivedAt: '2030-01-01T10:00:00Z',
  };

  beforeEach(() => {
    pharmacyService = jasmine.createSpyObj<PharmacyService>('PharmacyService', [
      'findExpiringSoon',
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    pharmacyService.findExpiringSoon.and.returnValue(of([batch]));
    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestor',
      username: 'gestor',
      email: 'gestor@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });

    TestBed.configureTestingModule({
      imports: [ExpiringStockComponent],
      providers: [
        { provide: PharmacyService, useValue: pharmacyService },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(ExpiringStockComponent).componentInstance;
    component.ngOnInit();
  });

  it('carrega os alertas de validade no período seleccionado', () => {
    expect(pharmacyService.findExpiringSoon).toHaveBeenCalledWith(30);
    expect(component.batches).toEqual([batch]);
    expect(component.loading).toBeFalse();
  });

  it('marca como crítica a validade entre hoje e os próximos sete dias', () => {
    const today = todayInLuanda();
    const date = new Date(`${today}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + 7);
    const expiryDate = date.toISOString().slice(0, 10);

    expect(component.isCritical({ ...batch, expiryDate })).toBeTrue();
    expect(
      component.isCritical({
        ...batch,
        expiryDate: new Date(Date.parse(`${today}T00:00:00Z`) + 8 * 86_400_000)
          .toISOString()
          .slice(0, 10),
      }),
    ).toBeFalse();
  });

  it('mostra erros de carregamento sem ocultá-los', () => {
    pharmacyService.findExpiringSoon.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Serviço de farmácia indisponível.' },
            status: 503,
          }),
      ),
    );

    component.load();

    expect(component.error).toBe('Serviço de farmácia indisponível.');
    expect(component.batches).toEqual([]);
    expect(component.loading).toBeFalse();
  });
});
