import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PatientResponse, PatientService } from '../../../core/services/patient.service';
import { PatientsListComponent } from './patients-list.component';

describe('PatientsListComponent', () => {
  let component: PatientsListComponent;
  let patientService: jasmine.SpyObj<PatientService>;

  beforeEach(() => {
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    patientService.findAll.and.returnValue(
      of({
        content: [{ id: 'patient-1', fullName: 'Ana Silva' } as PatientResponse],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );

    TestBed.configureTestingModule({
      imports: [PatientsListComponent],
      providers: [provideRouter([]), { provide: PatientService, useValue: patientService }],
    });

    component = TestBed.createComponent(PatientsListComponent).componentInstance;
    component.ngOnInit();
  });

  it('carrega a página inicial de pacientes', () => {
    expect(patientService.findAll).toHaveBeenCalledWith('', 0, 20);
    expect(component.patients[0].fullName).toBe('Ana Silva');
    expect(component.totalElements).toBe(1);
  });

  it('apresenta os géneros em português', () => {
    expect(component.getGenderLabel('MALE')).toBe('Masculino');
    expect(component.getGenderLabel('FEMALE')).toBe('Feminino');
  });
});
