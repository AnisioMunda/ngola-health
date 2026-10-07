import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PatientResponse, PatientService } from '../../../core/services/patient.service';
import { ReportService } from '../../../core/services/report.service';
import { PatientsListComponent } from './patients-list.component';

describe('PatientsListComponent', () => {
  let component: PatientsListComponent;
  let patientService: jasmine.SpyObj<PatientService>;
  let reportService: jasmine.SpyObj<ReportService>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    reportService = jasmine.createSpyObj<ReportService>('ReportService', [
      'downloadPatientReport',
      'openOrDownload',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    patientService.findAll.and.returnValue(
      of({
        content: [{ id: 'patient-1', fullName: 'Ana Silva' } as PatientResponse],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );
    reportService.downloadPatientReport.and.returnValue(
      of(new Blob(['pdf'], { type: 'application/pdf' })),
    );
    authService.getCurrentUser.and.returnValue({
      id: 'user-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });

    TestBed.configureTestingModule({
      imports: [PatientsListComponent],
      providers: [
        provideRouter([]),
        { provide: PatientService, useValue: patientService },
        { provide: ReportService, useValue: reportService },
        { provide: AuthService, useValue: authService },
      ],
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

  it('permite gerar a ficha PDF apenas a perfis autorizados', () => {
    expect(component.canDownloadPatientReport).toBeTrue();

    authService.getCurrentUser.and.returnValue({
      id: 'user-2',
      fullName: 'Recepcionista',
      username: 'recepcao',
      email: 'recepcao@example.ao',
      roles: ['RECEPTIONIST'],
      mustChangePassword: false,
    });
    expect(component.canDownloadPatientReport).toBeFalse();
  });

  it('descarrega a ficha do paciente sem activar a navegação da linha', () => {
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.downloadPatientReport('patient-1', event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(reportService.downloadPatientReport).toHaveBeenCalledWith('patient-1');
    expect(reportService.openOrDownload).toHaveBeenCalledWith(
      jasmine.any(Blob),
      'ficha-paciente-patient-1.pdf',
    );
    expect(component.downloadingPatientReport).toBeFalse();
  });
});
