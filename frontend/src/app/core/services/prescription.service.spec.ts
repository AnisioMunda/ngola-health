import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';
import {
  PrescriptionService,
  PrescriptionResponse,
  PrescriptionStatus
} from './prescription.service';
import { environment } from '../../../environments/environment';

describe('PrescriptionService', () => {
  let service: PrescriptionService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/prescriptions`;

  const mockPrescription: PrescriptionResponse = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    prescriptionNumber: 'RX-2026-00001',
    patientId: 'patient-uuid',
    patientName: 'João Silva',
    doctorId: 'doctor-uuid',
    doctorName: 'Dr. António Costa',
    episodeId: '',
    admissionId: '',
    status: 'ACTIVE' as PrescriptionStatus,
    statusLabel: 'Activa',
    prescriptionDate: '2026-08-01',
    expiryDate: '2026-08-31',
    expired: false,
    diagnosis: 'Infecção bacteriana',
    notes: '',
    cancelledReason: '',
    items: [],
    dispensations: [],
    createdAt: '2026-08-01T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PrescriptionService]
    });
    service     = TestBed.inject(PrescriptionService);
    httpMock    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { httpMock.verify(); });

  // ------------------------------------------------
  // getStats()
  // ------------------------------------------------

  it('deve chamar GET /stats e retornar estatísticas', () => {
    const mockStats = {
      totalActive: 5, totalToday: 2, pendingDispense: 3, expiringSoon: 1
    };

    service.getStats().subscribe(stats => {
      expect(stats.totalActive).toBe(5);
      expect(stats.pendingDispense).toBe(3);
    });

    const req = httpMock.expectOne(`${apiUrl}/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });

  // ------------------------------------------------
  // findAll()
  // ------------------------------------------------

  it('deve chamar GET / com parâmetros de paginação', () => {
    const mockPage = {
      content: [mockPrescription],
      totalElements: 1,
      totalPages: 1,
      number: 0
    };

    service.findAll('2026-08-01', '2026-08-31', 0).subscribe(page => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].prescriptionNumber).toBe('RX-2026-00001');
    });

    const req = httpMock.expectOne(r =>
      r.url === apiUrl && r.params.has('from') && r.params.has('to')
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('from')).toBe('2026-08-01');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(mockPage);
  });

  it('deve chamar GET / sem filtro de data', () => {
    const mockPage = {
      content: [], totalElements: 0, totalPages: 0, number: 0
    };

    service.findAll(undefined, undefined, 0).subscribe(page => {
      expect(page.content).toEqual([]);
    });

    const req = httpMock.expectOne(r =>
      r.url === apiUrl && !r.params.has('from')
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  // ------------------------------------------------
  // findById()
  // ------------------------------------------------

  it('deve chamar GET /{id}', () => {
    const id = mockPrescription.id;

    service.findById(id).subscribe(p => {
      expect(p.id).toBe(id);
      expect(p.status).toBe('ACTIVE');
    });

    const req = httpMock.expectOne(`${apiUrl}/${id}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPrescription);
  });

  // ------------------------------------------------
  // findByPatient()
  // ------------------------------------------------

  it('deve chamar GET /patient/{patientId}', () => {
    const patientId = 'patient-uuid';
    const mockPage  = { content: [mockPrescription], totalElements: 1, totalPages: 1, number: 0 };

    service.findByPatient(patientId).subscribe(page => {
      expect(page.content[0].patientId).toBe(patientId);
    });

    const req = httpMock.expectOne(r =>
      r.url === `${apiUrl}/patient/${patientId}`
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  // ------------------------------------------------
  // create()
  // ------------------------------------------------

  it('deve chamar POST / com o payload correcto', () => {
    const reqBody = {
      patientId: 'patient-uuid',
      validityDays: 30,
      items: [{
        medicationId: 'med-uuid',
        quantityPrescribed: 10,
        dosage: '1 comprimido 3x ao dia'
      }]
    };

    service.create(reqBody).subscribe(p => {
      expect(p.prescriptionNumber).toBe('RX-2026-00001');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.patientId).toBe('patient-uuid');
    expect(req.request.body.items.length).toBe(1);
    req.flush(mockPrescription);
  });

  // ------------------------------------------------
  // dispense()
  // ------------------------------------------------

  it('deve chamar POST /{id}/dispense', () => {
    const id = mockPrescription.id;
    const dispensed = { ...mockPrescription, status: 'DISPENSED' as PrescriptionStatus };

    service.dispense(id, { prescriptionItemId: 'item-uuid', quantityToDispense: 5 })
      .subscribe(p => {
        expect(p.status).toBe('DISPENSED');
      });

    const req = httpMock.expectOne(`${apiUrl}/${id}/dispense`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.quantityToDispense).toBe(5);
    req.flush(dispensed);
  });

  // ------------------------------------------------
  // cancel()
  // ------------------------------------------------

  it('deve chamar PATCH /{id}/cancel com motivo', () => {
    const id        = mockPrescription.id;
    const cancelled = { ...mockPrescription, status: 'CANCELLED' as PrescriptionStatus };

    service.cancel(id, 'Alergia ao medicamento').subscribe(p => {
      expect(p.status).toBe('CANCELLED');
    });

    const req = httpMock.expectOne(`${apiUrl}/${id}/cancel`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.reason).toBe('Alergia ao medicamento');
    req.flush(cancelled);
  });

  // ------------------------------------------------
  // findByEpisode()
  // ------------------------------------------------

  it('deve chamar GET /episode/{episodeId}', () => {
    const episodeId = 'episode-uuid';

    service.findByEpisode(episodeId).subscribe(list => {
      expect(list.length).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/episode/${episodeId}`);
    expect(req.request.method).toBe('GET');
    req.flush([mockPrescription]);
  });
});