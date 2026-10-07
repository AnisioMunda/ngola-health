import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TriageService, TriageResponse } from './triage.service';
import { environment } from '../../../environments/environment';

describe('TriageService', () => {
  let service: TriageService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/triage`;

  const mockTriage: TriageResponse = {
    id: 'triage-uuid',
    queueNumber: 1,
    patientId: 'patient-uuid',
    patientName: 'José Manuel',
    patientAge: 35,
    patientGender: 'M',
    priority: 'YELLOW',
    priorityLabel: 'Urgente',
    priorityColor: '#ca8a04',
    chiefComplaint: 'Dor abdominal intensa',
    bloodPressure: '130/85',
    heartRate: 90,
    temperature: 38.5,
    oxygenSaturation: 97,
    respiratoryRate: 18,
    weightKg: 75,
    painScale: 7,
    triageNotes: '',
    status: 'WAITING',
    statusLabel: 'Em Espera',
    triagedByName: 'Enfermeira Ana',
    episodeId: '',
    triagedAt: '2026-08-30T09:00:00Z',
    attendedAt: '',
    completedAt: '',
    waitingMinutes: 15,
    overdue: false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TriageService],
    });
    service = TestBed.inject(TriageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve retornar estatísticas de triagem', () => {
    const stats = {
      totalWaiting: 3,
      totalToday: 10,
      inProgress: 1,
      completedToday: 6,
      waitingRed: 1,
      waitingOrange: 0,
      waitingYellow: 2,
      waitingGreen: 0,
      waitingBlue: 0,
      avgWaitingMinutes: 22.5,
    };

    service.getStats().subscribe((s) => {
      expect(s.totalWaiting).toBe(3);
      expect(s.waitingRed).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(stats);
  });

  it('deve retornar fila activa', () => {
    service.getActiveQueue().subscribe((q) => {
      expect(q.length).toBe(1);
      expect(q[0].priority).toBe('YELLOW');
    });

    const req = httpMock.expectOne(`${apiUrl}/queue`);
    req.flush([mockTriage]);
  });

  it('deve chamar paciente', () => {
    const called = { ...mockTriage, status: 'IN_PROGRESS' as any };

    service.callNext(mockTriage.id).subscribe((t) => {
      expect(t.status).toBe('IN_PROGRESS');
    });

    const req = httpMock.expectOne(`${apiUrl}/${mockTriage.id}/call`);
    expect(req.request.method).toBe('PATCH');
    req.flush(called);
  });

  it('deve completar atendimento', () => {
    const completed = { ...mockTriage, status: 'COMPLETED' as any };

    service.complete(mockTriage.id).subscribe((t) => {
      expect(t.status).toBe('COMPLETED');
    });

    const req = httpMock.expectOne(`${apiUrl}/${mockTriage.id}/complete`);
    expect(req.request.method).toBe('PATCH');
    req.flush(completed);
  });

  it('deve criar triagem', () => {
    const req = {
      patientId: 'patient-uuid',
      priority: 'RED',
      chiefComplaint: 'Dor no peito',
    };

    service.create(req).subscribe((t) => {
      expect(t.id).toBe('triage-uuid');
    });

    const httpReq = httpMock.expectOne(apiUrl);
    expect(httpReq.request.method).toBe('POST');
    expect(httpReq.request.body.priority).toBe('RED');
    httpReq.flush(mockTriage);
  });
});
