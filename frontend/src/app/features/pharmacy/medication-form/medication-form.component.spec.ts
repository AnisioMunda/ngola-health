import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { MedicationResponse, PharmacyService } from '../../../core/services/pharmacy.service';
import { MedicationFormComponent } from './medication-form.component';

describe('MedicationFormComponent', () => {
  let component: MedicationFormComponent;
  let pharmacyService: jasmine.SpyObj<PharmacyService>;
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    pharmacyService = jasmine.createSpyObj<PharmacyService>('PharmacyService', [
      'createMedication',
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    pharmacyService.createMedication.and.returnValue(
      of({
        id: 'med-1',
        name: 'Amoxicilina',
        genericName: null,
        dosageForm: 'CAPSULE',
        strength: '500 mg',
        unit: 'cápsula',
        requiresPrescription: true,
        minStockLevel: 10,
        totalAvailable: 0,
        active: true,
      } satisfies MedicationResponse),
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
      imports: [MedicationFormComponent],
      providers: [
        { provide: PharmacyService, useValue: pharmacyService },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(MedicationFormComponent).componentInstance;
  });

  it('envia os valores normalizados e volta à lista após criar', () => {
    component.ngOnInit();
    component.form.setValue({
      name: '  Amoxicilina  ',
      genericName: '  ',
      dosageForm: 'CAPSULE',
      strength: ' 500 mg ',
      unit: ' cápsula ',
      requiresPrescription: true,
      minStockLevel: 10,
    });

    component.onSubmit();

    expect(pharmacyService.createMedication).toHaveBeenCalledWith({
      name: 'Amoxicilina',
      genericName: null,
      dosageForm: 'CAPSULE',
      strength: '500 mg',
      unit: 'cápsula',
      requiresPrescription: true,
      minStockLevel: 10,
    });
    expect(router.navigate).toHaveBeenCalledWith(['/pharmacy']);
    expect(component.saving).toBeFalse();
  });

  it('não envia dados inválidos nem apresenta sucesso falso quando a API falha', () => {
    component.ngOnInit();
    component.onSubmit();
    expect(pharmacyService.createMedication).not.toHaveBeenCalled();

    pharmacyService.createMedication.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'O medicamento já existe.' },
            status: 409,
          }),
      ),
    );
    component.form.setValue({
      name: 'Amoxicilina',
      genericName: '',
      dosageForm: 'CAPSULE',
      strength: '',
      unit: 'cápsula',
      requiresPrescription: true,
      minStockLevel: 10,
    });
    component.onSubmit();

    expect(component.error).toBe('O medicamento já existe.');
    expect(component.saving).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirecciona gestores e impede-lhes a criação de medicamentos', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestor',
      username: 'gestor',
      email: 'gestor@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });

    component.ngOnInit();
    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/pharmacy']);
    expect(pharmacyService.createMedication).not.toHaveBeenCalled();
  });
});
