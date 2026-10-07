import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { HospitalManagementService } from './hospital-management.service';

describe('HospitalManagementService', () => {
  let service: HospitalManagementService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), HospitalManagementService],
    });
    service = TestBed.inject(HospitalManagementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads active and inactive hospitals from the management endpoint', () => {
    let response: unknown;
    service.findAllForManagement().subscribe((hospitals) => (response = hospitals));

    const request = httpMock.expectOne(`${environment.apiUrl}/hospitals/management`);
    expect(request.request.method).toBe('GET');
    request.flush([]);

    expect(response).toEqual([]);
  });
});
