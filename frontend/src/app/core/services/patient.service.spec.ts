import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PatientDuplicateCandidate, PatientService } from './patient.service';
import { environment } from '../../../environments/environment';

describe('PatientService', () => {
  let service: PatientService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/patients`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PatientService],
    });
    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('verifica possíveis duplicados antes do cadastro', () => {
    const request = {
      fullName: 'Ana Silva',
      birthDate: '1990-04-12',
      phone: '+244 923 000 000',
    };
    const candidates: PatientDuplicateCandidate[] = [
      { id: 'patient-1', fullName: 'Ana Silva', birthDate: '1990-04-12' },
    ];

    service.findPossibleDuplicates(request).subscribe((response) => {
      expect(response).toEqual(candidates);
    });

    const httpRequest = httpMock.expectOne(`${apiUrl}/possible-duplicates`);
    expect(httpRequest.request.method).toBe('POST');
    expect(httpRequest.request.body).toEqual(request);
    httpRequest.flush(candidates);
  });
});
