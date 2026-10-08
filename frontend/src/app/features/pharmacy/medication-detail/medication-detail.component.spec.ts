import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import {
  MedicationResponse,
  PharmacyService,
  StockBatchResponse,
} from '../../../core/services/pharmacy.service';
import { MedicationDetailComponent } from './medication-detail.component';

describe('MedicationDetailComponent', () => {
  let component: MedicationDetailComponent;
  let pharmacyService: jasmine.SpyObj<PharmacyService>;
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;

  const medication: MedicationResponse = {
    id: 'med-1',
    name: 'Amoxicilina',
    genericName: null,
    dosageForm: 'CAPSULE',
    strength: '500 mg',
    unit: 'cápsula',
    requiresPrescription: true,
    minStockLevel: 10,
    totalAvailable: 12,
    active: true,
  };
  const batch: StockBatchResponse = {
    id: 'batch-1',
    medicationId: medication.id,
    medicationName: medication.name,
    batchNumber: 'L-001',
    expiryDate: '2030-05-01',
    quantityReceived: 20,
    quantityAvailable: 12,
    unitCost: null,
    supplier: null,
    expired: false,
    expiringSoon: false,
    receivedAt: '2030-01-01T10:00:00Z',
  };

  beforeEach(() => {
    pharmacyService = jasmine.createSpyObj<PharmacyService>('PharmacyService', [
      'findMedication',
      'findBatches',
      'receiveStock',
      'dispense',
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    pharmacyService.findMedication.and.returnValue(of(medication));
    pharmacyService.findBatches.and.returnValue(of([batch]));
    pharmacyService.receiveStock.and.returnValue(of(batch));
    pharmacyService.dispense.and.returnValue(
      of({
        medicationId: medication.id,
        medicationName: medication.name,
        quantityDispensed: 2,
        remainingStock: 10,
      }),
    );
    authService.getCurrentUser.and.returnValue({
      id: 'pharmacist-1',
      fullName: 'Farmacêutica',
      username: 'farmaceutica',
      email: 'farmacia@example.ao',
      roles: ['PHARMACIST'],
      mustChangePassword: false,
    });

    TestBed.configureTestingModule({
      imports: [MedicationDetailComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'med-1' } } } },
        { provide: PharmacyService, useValue: pharmacyService },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(MedicationDetailComponent).componentInstance;
    component.ngOnInit();
  });

  afterEach(() => component.ngOnDestroy());

  it('carrega os dados do medicamento e respectivos lotes', () => {
    expect(component.medication).toEqual(medication);
    expect(component.batches).toEqual([batch]);
    expect(component.isLowStock()).toBe(false);
  });

  it('regista uma entrada de stock e actualiza os dados', () => {
    component.receiveForm.setValue({
      batchNumber: ' L-002 ',
      expiryDate: '2030-12-31',
      quantity: 15,
      unitCost: 25,
      supplier: ' Fornecedor ',
    });

    component.receiveStock();

    expect(pharmacyService.receiveStock).toHaveBeenCalledWith({
      medicationId: medication.id,
      batchNumber: 'L-002',
      expiryDate: '2030-12-31',
      quantity: 15,
      unitCost: 25,
      supplier: 'Fornecedor',
    });
    expect(component.batchesLoading).toBe(false);
    expect(component.error).toBe('');
  });

  it('permite consulta ao gestor, mas bloqueia alterações de stock', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestor',
      username: 'gestor',
      email: 'gestor@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });
    component.receiveForm.setValue({
      batchNumber: 'L-002',
      expiryDate: '2030-12-31',
      quantity: 15,
      unitCost: null,
      supplier: '',
    });

    expect(component.canViewStockDetails).toBe(true);
    expect(component.canManageStock).toBe(false);
    component.receiveStock();

    expect(pharmacyService.receiveStock).not.toHaveBeenCalled();
    expect(component.medication).toEqual(medication);
  });

  it('apresenta o erro da API ao falhar o carregamento dos lotes', () => {
    pharmacyService.findBatches.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não autorizado para consultar os lotes.' },
            status: 403,
          }),
      ),
    );

    component.loadBatches();

    expect(component.error).toBe('Não autorizado para consultar os lotes.');
    expect(component.batches).toEqual([batch]);
    expect(component.batchesLoading).toBe(false);
  });
});
