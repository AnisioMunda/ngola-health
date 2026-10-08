import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PatientService } from '../../../core/services/patient.service';
import {
  CreateTriageRequest,
  TriageResponse,
  TriageService,
  TriageStatsDto,
} from '../../../core/services/triage.service';
import { TriageComponent } from './triage.component';

describe('TriageComponent', () => {
  let component: TriageComponent;
  let triageService: jasmine.SpyObj<TriageService>;
  let patientService: jasmine.SpyObj<PatientService>;

  const triage: TriageResponse = {
    id: 'triage-1',
    queueNumber: 1,
    patientId: null,
    patientName: 'Paciente de teste',
    patientAge: null,
    patientGender: null,
    priority: 'GREEN',
    priorityLabel: 'Pouco Urgente',
    priorityColor: '#16a34a',
    chiefComplaint: 'Dor de cabeça',
    bloodPressure: null,
    heartRate: null,
    temperature: null,
    oxygenSaturation: null,
    respiratoryRate: null,
    weightKg: null,
    painScale: null,
    triageNotes: null,
    status: 'WAITING',
    statusLabel: 'Em Espera',
    triagedByName: null,
    episodeId: null,
    triagedAt: '2026-08-30T09:00:00Z',
    attendedAt: null,
    completedAt: null,
    waitingMinutes: 0,
    overdue: false,
  };

  const stats: TriageStatsDto = {
    totalWaiting: 0,
    totalToday: 0,
    inProgress: 0,
    completedToday: 0,
    waitingRed: 0,
    waitingOrange: 0,
    waitingYellow: 0,
    waitingGreen: 0,
    waitingBlue: 0,
    avgWaitingMinutes: 0,
  };

  beforeEach(() => {
    triageService = jasmine.createSpyObj<TriageService>('TriageService', [
      'getActiveQueue',
      'getStats',
      'getHistory',
      'create',
      'updatePriority',
      'callNext',
      'complete',
      'markAsLeft',
    ]);
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    triageService.getActiveQueue.and.returnValue(of([]));
    triageService.getStats.and.returnValue(of(stats));
    triageService.getHistory.and.returnValue(of([]));
    triageService.create.and.returnValue(of(triage));
    triageService.updatePriority.and.returnValue(of(triage));
    triageService.callNext.and.returnValue(of(triage));
    triageService.complete.and.returnValue(of(triage));
    triageService.markAsLeft.and.returnValue(of(triage));
    patientService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 200 }),
    );

    TestBed.configureTestingModule({
      imports: [TriageComponent],
      providers: [
        { provide: TriageService, useValue: triageService },
        { provide: PatientService, useValue: patientService },
      ],
    });

    component = TestBed.createComponent(TriageComponent).componentInstance;
    component.ngOnInit();
  });

  afterEach(() => component.ngOnDestroy());

  it('envia os dados tipados e preserva a escala de dor zero', () => {
    component.form.patchValue({
      patientNameTemp: '  Paciente temporário  ',
      priority: 'RED',
      chiefComplaint: '  Dor torácica  ',
      painScale: 0,
    });

    component.onSubmit();

    const request: CreateTriageRequest = triageService.create.calls.mostRecent().args[0];
    expect(request).toEqual({
      patientId: null,
      patientNameTemp: 'Paciente temporário',
      patientAgeTemp: null,
      patientGenderTemp: null,
      priority: 'RED',
      chiefComplaint: 'Dor torácica',
      bloodPressure: null,
      heartRate: null,
      temperature: null,
      oxygenSaturation: null,
      respiratoryRate: null,
      weightKg: null,
      painScale: 0,
      triageNotes: null,
    });
    expect(component.saving).toBe(false);
  });

  it('exibe detalhes de erro da API ao registar triagem', () => {
    triageService.create.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'A prioridade seleccionada é inválida.' },
            status: 422,
          }),
      ),
    );
    component.form.patchValue({
      patientNameTemp: 'Paciente temporário',
      chiefComplaint: 'Dor',
    });

    component.onSubmit();

    expect(component.error).toBe('A prioridade seleccionada é inválida.');
    expect(component.saving).toBe(false);
  });

  it('limpa os dados temporários ao seleccionar um paciente registado', () => {
    component.form.patchValue({
      patientId: 'patient-1',
      patientNameTemp: 'Nome temporário',
      patientAgeTemp: 44,
      patientGenderTemp: 'F',
    });

    component.onPatientChanged();

    expect(component.form.getRawValue()).toEqual(
      jasmine.objectContaining({
        patientId: 'patient-1',
        patientNameTemp: '',
        patientAgeTemp: null,
        patientGenderTemp: '',
      }),
    );
  });

  it('apresenta falhas ao actualizar a fila e liberta o estado de acção', () => {
    triageService.callNext.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'A triagem já foi chamada.' },
            status: 409,
          }),
      ),
    );

    component.callNext(triage);

    expect(component.error).toBe('A triagem já foi chamada.');
    expect(component.isUpdating(triage.id)).toBe(false);
  });

  it('apresenta falhas ao carregar o histórico sem indicar falsamente que está vazio', () => {
    triageService.getHistory.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Histórico indisponível.' },
            status: 503,
          }),
      ),
    );

    component.onTabChange('history');

    expect(component.historyError).toBe('Histórico indisponível.');
    expect(component.historyLoading).toBe(false);
  });

  it('não permite alterar a prioridade de uma triagem já chamada', () => {
    component.updatePriority({ ...triage, status: 'IN_PROGRESS' }, 'RED');

    expect(triageService.updatePriority).not.toHaveBeenCalled();
  });
});
