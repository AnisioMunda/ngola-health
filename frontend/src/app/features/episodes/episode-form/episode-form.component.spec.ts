import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { EpisodeResponse, EpisodeService } from '../../../core/services/episode.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { EpisodeFormComponent } from './episode-form.component';

describe('EpisodeFormComponent', () => {
  let component: EpisodeFormComponent;
  let episodeService: jasmine.SpyObj<EpisodeService>;
  let patientService: jasmine.SpyObj<PatientService>;
  let userService: jasmine.SpyObj<UserManagementService>;
  let router: jasmine.SpyObj<Router>;

  const emptyEpisode: EpisodeResponse = {
    id: 'episode-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    doctorId: null,
    doctorName: null,
    episodeType: 'OUTPATIENT',
    status: 'SCHEDULED',
    scheduledAt: null,
    startedAt: null,
    completedAt: null,
    reason: null,
    symptoms: null,
    diagnosis: null,
    prescription: null,
    notes: null,
    bloodPressure: null,
    heartRate: null,
    temperature: null,
    weightKg: null,
    createdAt: '',
  };

  beforeEach(() => {
    episodeService = jasmine.createSpyObj<EpisodeService>('EpisodeService', [
      'create',
      'update',
      'findById',
    ]);
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', ['findAll']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    patientService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
    );
    userService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
    );
    episodeService.create.and.returnValue(of(emptyEpisode));
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      imports: [EpisodeFormComponent],
      providers: [
        { provide: EpisodeService, useValue: episodeService },
        { provide: PatientService, useValue: patientService },
        { provide: UserManagementService, useValue: userService },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => null } } },
        },
      ],
    });

    const fixture = TestBed.createComponent(EpisodeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submete dados de criação com texto normalizado e tipos correctos', () => {
    component.form.patchValue({
      patientId: 'patient-1',
      episodeType: 'OUTPATIENT',
      reason: '  Consulta de rotina  ',
      symptoms: '  Tosse  ',
      heartRate: 72,
      temperature: 36.5,
    });

    component.onSubmit();

    expect(episodeService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        patientId: 'patient-1',
        episodeType: 'OUTPATIENT',
        reason: 'Consulta de rotina',
        symptoms: 'Tosse',
        heartRate: 72,
        temperature: 36.5,
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/episodes']);
  });

  it('impede a submissão sem seleccionar um paciente', () => {
    component.onSubmit();

    expect(episodeService.create).not.toHaveBeenCalled();
    expect(component.form.controls['patientId'].touched).toBe(true);
  });
});
