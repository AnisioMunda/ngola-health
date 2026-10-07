import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { ReportService } from './report.service';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReportService],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests the patient record as a PDF blob', () => {
    const expectedBlob = new Blob(['pdf'], { type: 'application/pdf' });
    let actualBlob: Blob | undefined;

    service.downloadPatientReport('patient-1').subscribe((blob) => (actualBlob = blob));

    const request = httpMock.expectOne(`${environment.apiUrl}/reports/patients/patient-1`);
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(expectedBlob);

    expect(actualBlob).toBe(expectedBlob);
  });
});
