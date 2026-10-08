import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import {
  PrescriptionResponse,
  PrescriptionService,
} from '../../../core/services/prescription.service';
import { PrescriptionDetailComponent } from './prescription-detail.component';

describe('PrescriptionDetailComponent', () => {
  let component: PrescriptionDetailComponent;
  let prescriptionService: jasmine.SpyObj<PrescriptionService>;
  let authService: jasmine.SpyObj<AuthService>;

  const prescription: PrescriptionResponse = {
    id: 'prescription-1',
    prescriptionNumber: 'RX-2026-00001',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    doctorId: 'doctor-1',
    doctorName: 'Médica',
    episodeId: null,
    admissionId: null,
    status: 'ACTIVE',
    statusLabel: 'Activa',
    prescriptionDate: '2026-08-01',
    expiryDate: '2026-08-31',
    expired: false,
    diagnosis: null,
    notes: null,
    cancelledReason: null,
    createdAt: null,
    items: [
      {
        id: 'item-1',
        medicationId: 'medication-1',
        medicationName: 'Amoxicilina',
        medicationUnit: 'cápsula',
        quantityPrescribed: 10,
        quantityDispensed: 0,
        remainingQuantity: 10,
        dosage: '1 cápsula',
        frequencyHours: 8,
        durationDays: 7,
        route: 'Oral',
        instructions: null,
        status: 'PENDING',
        statusLabel: 'Pendente',
        stockAvailable: 5,
      },
    ],
    dispensations: [],
  };

  beforeEach(() => {
    prescriptionService = jasmine.createSpyObj<PrescriptionService>('PrescriptionService', [
      'findById',
      'dispense',
      'cancel',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    prescriptionService.findById.and.returnValue(of(prescription));
    authService.getCurrentUser.and.returnValue({
      id: 'pharmacist-1',
      fullName: 'Farmacêutica',
      username: 'farmaceutica',
      email: 'farmacia@example.ao',
      roles: ['PHARMACIST'],
      mustChangePassword: false,
    });
    TestBed.configureTestingModule({
      imports: [PrescriptionDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'prescription-1' } } },
        },
        { provide: PrescriptionService, useValue: prescriptionService },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(PrescriptionDetailComponent).componentInstance;
    component.ngOnInit();
  });

  afterEach(() => component.ngOnDestroy());

  it('permite dispensar ao farmacêutico, mas não permite cancelar', () => {
    expect(component.canDispense()).toBe(true);
    expect(component.canCancel()).toBe(false);
  });

  it('restringe acções de acordo com o perfil e a disponibilidade', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });
    expect(component.canDispense()).toBe(false);
    expect(component.canCancel()).toBe(true);

    authService.getCurrentUser.and.returnValue({
      id: 'pharmacist-1',
      fullName: 'Farmacêutica',
      username: 'farmaceutica',
      email: 'farmacia@example.ao',
      roles: ['PHARMACIST'],
      mustChangePassword: false,
    });
    component.openDispense(prescription.items[0]);
    component.quantityToDispense = 6;
    component.dispense();

    expect(component.error).toContain('stock disponível');
    expect(prescriptionService.dispense).not.toHaveBeenCalled();
  });
});
