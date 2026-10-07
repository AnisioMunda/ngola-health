import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PatientResponse, PatientService } from '../../../core/services/patient.service';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionResponse,
  PrescriptionService,
} from '../../../core/services/prescription.service';
import { PrescriptionFormComponent } from './prescription-form.component';

describe('PrescriptionFormComponent', () => {
  let component: PrescriptionFormComponent;
  let prescriptionService: jasmine.SpyObj<PrescriptionService>;
  let patientService: jasmine.SpyObj<PatientService>;
  let pharmacyService: jasmine.SpyObj<PharmacyService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    prescriptionService = jasmine.createSpyObj<PrescriptionService>('PrescriptionService', [
      'create',
    ]);
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    pharmacyService = jasmine.createSpyObj<PharmacyService>('PharmacyService', [
      'findAllMedications',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    patientService.findAll.and.returnValue(
      of({
        content: [{ id: 'patient-1', fullName: 'Ana Silva' } as PatientResponse],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );
    pharmacyService.findAllMedications.and.returnValue(
      of({
        content: [
          {
            id: 'medication-1',
            name: 'Amoxicilina',
            unit: 'cápsula',
            totalAvailable: 20,
            active: true,
            genericName: null,
            dosageForm: 'CAPSULE',
            strength: '500 mg',
            requiresPrescription: true,
            minStockLevel: 5,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );
    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });
    prescriptionService.create.and.returnValue(
      of({ id: 'prescription-1' } as PrescriptionResponse),
    );

    TestBed.configureTestingModule({
      imports: [PrescriptionFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
        { provide: Router, useValue: router },
        { provide: PrescriptionService, useValue: prescriptionService },
        { provide: PatientService, useValue: patientService },
        { provide: PharmacyService, useValue: pharmacyService },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(PrescriptionFormComponent).componentInstance;
  });

  it('carrega os dados necessários e envia um pedido tipado', () => {
    component.ngOnInit();
    component.form.controls.patientId.setValue('patient-1');
    const item = component.items.at(0);
    item.controls.medicationId.setValue('medication-1');
    item.controls.dosage.setValue('  1 cápsula de 8 em 8 horas  ');

    component.onSubmit();

    expect(patientService.findAll).toHaveBeenCalledWith('', 0, 200);
    expect(pharmacyService.findAllMedications).toHaveBeenCalledWith('', 0, 200);
    expect(prescriptionService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        patientId: 'patient-1',
        diagnosis: null,
        validityDays: 30,
        items: [
          jasmine.objectContaining({
            medicationId: 'medication-1',
            quantityPrescribed: 1,
            dosage: '1 cápsula de 8 em 8 horas',
          }),
        ],
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/prescriptions', 'prescription-1']);
  });

  it('apresenta falha de carregamento e bloqueia submissão até poder tentar novamente', () => {
    patientService.findAll.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não foi possível consultar pacientes.' },
            status: 503,
          }),
      ),
    );
    component.ngOnInit();
    component.form.controls.patientId.setValue('patient-1');
    component.items.at(0).controls.medicationId.setValue('medication-1');
    component.items.at(0).controls.dosage.setValue('1 cápsula');

    component.onSubmit();

    expect(component.patientsError).toBe('Não foi possível consultar pacientes.');
    expect(prescriptionService.create).not.toHaveBeenCalled();
  });

  it('redirecciona perfis sem permissão de emissão', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'pharmacist-1',
      fullName: 'Farmacêutica',
      username: 'farmaceutica',
      email: 'farmacia@example.ao',
      roles: ['PHARMACIST'],
      mustChangePassword: false,
    });

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/prescriptions']);
    expect(patientService.findAll).not.toHaveBeenCalled();
    expect(pharmacyService.findAllMedications).not.toHaveBeenCalled();
  });
});
