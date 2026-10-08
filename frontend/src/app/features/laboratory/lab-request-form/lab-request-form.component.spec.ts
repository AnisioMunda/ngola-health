import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PatientResponse, PatientService } from '../../../core/services/patient.service';
import { LabRequestResponse, LabService } from '../../../core/services/lab.service';
import { LabRequestFormComponent } from './lab-request-form.component';

describe('LabRequestFormComponent', () => {
  let component: LabRequestFormComponent;
  let labService: jasmine.SpyObj<LabService>;
  let patientService: jasmine.SpyObj<PatientService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    labService = jasmine.createSpyObj<LabService>('LabService', ['findAllTests', 'createRequest']);
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    labService.findAllTests.and.returnValue(
      of([
        {
          id: 'test-1',
          code: 'HEM-01',
          name: 'Hemograma',
          category: 'HEMATOLOGY',
          sampleType: 'Sangue',
          turnaroundHours: 24,
          price: 1000,
          referenceValues: null,
          active: true,
        },
      ]),
    );
    labService.createRequest.and.returnValue(of({} as LabRequestResponse));
    patientService.findAll.and.returnValue(
      of({
        content: [{ id: 'patient-1', fullName: 'Ana Silva' } as PatientResponse],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 100,
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
    TestBed.configureTestingModule({
      imports: [LabRequestFormComponent],
      providers: [
        { provide: LabService, useValue: labService },
        { provide: PatientService, useValue: patientService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(LabRequestFormComponent).componentInstance;
  });

  it('carrega pacientes e exames e envia um pedido tipado', () => {
    component.ngOnInit();
    component.form.controls.patientId.setValue('patient-1');
    component.form.controls.clinicalNotes.setValue('  Jejum  ');
    component.toggleTest('test-1');

    component.onSubmit();

    expect(patientService.findAll).toHaveBeenCalledWith('', 0, 100);
    expect(labService.createRequest).toHaveBeenCalledWith({
      patientId: 'patient-1',
      priority: 'NORMAL',
      clinicalNotes: 'Jejum',
      labTestIds: ['test-1'],
    });
    expect(router.navigate).toHaveBeenCalledWith(['/lab']);
  });

  it('expõe erro de carregamento e não permite submeter sem o catálogo', () => {
    labService.findAllTests.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não foi possível consultar o catálogo.' },
            status: 503,
          }),
      ),
    );
    component.ngOnInit();
    component.form.controls.patientId.setValue('patient-1');
    component.toggleTest('test-1');

    component.onSubmit();

    expect(component.testsError).toBe('Não foi possível consultar o catálogo.');
    expect(labService.createRequest).not.toHaveBeenCalled();
  });

  it('redirecciona perfis sem permissão para criar pedidos', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestora',
      username: 'gestora',
      email: 'gestora@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });

    component.ngOnInit();

    expect(router.navigate).toHaveBeenCalledWith(['/lab']);
    expect(patientService.findAll).not.toHaveBeenCalled();
    expect(labService.findAllTests).not.toHaveBeenCalled();
  });
});
