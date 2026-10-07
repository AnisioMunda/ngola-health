import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { PatientService, PatientResponse } from '../../../core/services/patient.service';
import { PatientFormComponent } from './patient-form.component';

describe('PatientFormComponent', () => {
  let component: PatientFormComponent;
  let patientService: jasmine.SpyObj<PatientService>;
  let router: Router;

  const patientResponse = { id: 'patient-1' } as PatientResponse;

  beforeEach(() => {
    patientService = jasmine.createSpyObj<PatientService>('PatientService', [
      'findById',
      'findPossibleDuplicates',
      'create',
      'update',
    ]);
    patientService.findById.and.returnValue(of(patientResponse));
    patientService.findPossibleDuplicates.and.returnValue(of([]));
    patientService.create.and.returnValue(of(patientResponse));
    patientService.update.and.returnValue(of(patientResponse));

    TestBed.configureTestingModule({
      imports: [PatientFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
        { provide: PatientService, useValue: patientService },
      ],
    });

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    component = TestBed.createComponent(PatientFormComponent).componentInstance;
    component.ngOnInit();
  });

  it('mostra o separador com campos inválidos', () => {
    component.form.patchValue({
      fullName: 'Ana Silva',
      birthDate: '1990-04-12',
      gender: 'FEMALE',
      email: 'email-invalido',
    });

    component.onSubmit();

    expect(component.activeTab).toBe('contact');
    expect(patientService.findPossibleDuplicates).not.toHaveBeenCalled();
  });

  it('pede confirmação quando encontra um possível duplicado', () => {
    patientService.findPossibleDuplicates.and.returnValue(
      of([{ id: 'patient-1', fullName: 'Ana Silva', birthDate: '1990-04-12' }]),
    );
    component.form.patchValue({
      fullName: ' Ana Silva ',
      birthDate: '1990-04-12',
      gender: 'FEMALE',
    });

    component.onSubmit();

    expect(component.possibleDuplicates.length).toBe(1);
    expect(component.form.disabled).toBeTrue();
    expect(patientService.create).not.toHaveBeenCalled();

    component.confirmRegistrationDespiteDuplicates();

    expect(patientService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ fullName: 'Ana Silva' }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/patients']);
  });

  it('grava o paciente quando não encontra duplicados', () => {
    component.form.patchValue({
      fullName: 'Ana Silva',
      birthDate: '1990-04-12',
      gender: 'FEMALE',
    });

    component.onSubmit();

    expect(patientService.findPossibleDuplicates).toHaveBeenCalledWith(
      jasmine.objectContaining({ fullName: 'Ana Silva', birthDate: '1990-04-12' }),
    );
    expect(patientService.create).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/patients']);
  });

  it('cancela a confirmação e volta a permitir editar', () => {
    patientService.findPossibleDuplicates.and.returnValue(
      of([{ id: 'patient-1', fullName: 'Ana Silva', birthDate: '1990-04-12' }]),
    );
    component.form.patchValue({
      fullName: 'Ana Silva',
      birthDate: '1990-04-12',
      gender: 'FEMALE',
    });

    component.onSubmit();
    component.cancelRegistrationDespiteDuplicates();

    expect(component.possibleDuplicates.length).toBe(0);
    expect(component.form.enabled).toBeTrue();
    expect(patientService.create).not.toHaveBeenCalled();
  });
});
