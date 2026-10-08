import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import {
  AdmissionResponse,
  BedResponse,
  CreateAdmissionRequest,
  InpatientService,
  WardResponse,
} from '../../../core/services/inpatient.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { AdmissionFormComponent } from './admission-form.component';

describe('AdmissionFormComponent', () => {
  let inpatientService: jasmine.SpyObj<InpatientService>;
  let router: jasmine.SpyObj<Router>;
  let component: AdmissionFormComponent;

  const ward: WardResponse = {
    id: 'ward-1',
    name: 'Medicina Geral',
    code: 'MG',
    type: 'GENERAL',
    typeLabel: 'Medicina Geral',
    floor: null,
    totalBeds: 1,
    availableBeds: 1,
    occupiedBeds: 0,
    responsibleDoctorName: null,
    notes: null,
    active: true,
  };

  const bed: BedResponse = {
    id: 'bed-1',
    bedNumber: '01',
    status: 'AVAILABLE',
    statusLabel: 'Disponível',
    type: 'STANDARD',
    wardName: ward.name,
    wardId: ward.id,
    notes: null,
    patientName: null,
    admissionId: null,
    admissionDate: null,
  };

  const admission: AdmissionResponse = {
    id: 'admission-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    patientPhone: '',
    bedId: bed.id,
    bedNumber: bed.bedNumber,
    wardId: ward.id,
    wardName: ward.name,
    wardType: 'GENERAL',
    doctorId: 'doctor-1',
    doctorName: 'Dr. Silva',
    status: 'ACTIVE',
    statusLabel: 'Internado',
    admissionDate: '2026-10-07T10:00:00Z',
    expectedDischargeDate: null,
    dischargeDate: null,
    admissionReason: 'Dor intensa',
    diagnosis: '',
    dischargeNotes: null,
    dischargeCondition: null,
    daysAdmitted: 0,
    admittedByName: 'Admin',
    dischargedByName: null,
    createdAt: '2026-10-07T10:00:00Z',
    transfers: [],
  };

  beforeEach(() => {
    inpatientService = jasmine.createSpyObj<InpatientService>('InpatientService', [
      'findAllWards',
      'findBedsByWard',
      'admit',
    ]);
    inpatientService.findAllWards.and.returnValue(of([ward]));
    inpatientService.findBedsByWard.and.returnValue(of([bed]));
    inpatientService.admit.and.returnValue(of(admission));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [AdmissionFormComponent],
      providers: [
        { provide: InpatientService, useValue: inpatientService },
        {
          provide: PatientService,
          useValue: {
            findAll: () =>
              of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 200 }),
          },
        },
        {
          provide: UserManagementService,
          useValue: {
            findAll: () =>
              of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ wardId: ward.id, bedId: bed.id }),
            },
          },
        },
        { provide: Router, useValue: router },
      ],
    });

    const fixture = TestBed.createComponent(AdmissionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('pré-selecciona enfermaria e cama quando chega do mapa', () => {
    expect(component.form.controls.wardId.value).toBe(ward.id);
    expect(component.form.controls.bedId.value).toBe(bed.id);
    expect(component.beds).toEqual([bed]);
  });

  it('valida campos obrigatórios e envia a admissão com valores normalizados', () => {
    component.onSubmit();
    expect(inpatientService.admit).not.toHaveBeenCalled();

    component.form.patchValue({
      patientId: 'patient-1',
      responsibleDoctorId: 'doctor-1',
      admissionReason: '  Dor intensa  ',
      expectedDischargeDate: '2026-10-12',
    });
    component.onSubmit();

    const request: CreateAdmissionRequest = inpatientService.admit.calls.mostRecent().args[0];
    expect(request).toEqual({
      patientId: 'patient-1',
      bedId: bed.id,
      responsibleDoctorId: 'doctor-1',
      admissionReason: 'Dor intensa',
      expectedDischargeDate: '2026-10-12',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/inpatient/admissions', admission.id]);
    expect(component.saving).toBe(false);
  });

  it('apresenta a mensagem da API e liberta o estado de submissão', () => {
    inpatientService.admit.and.returnValue(
      throwError(() => ({ error: { detail: 'A cama acabou de ser ocupada.' } })),
    );
    component.form.patchValue({
      patientId: 'patient-1',
      responsibleDoctorId: 'doctor-1',
      admissionReason: 'Dor intensa',
    });

    component.onSubmit();

    expect(component.error).toBe('A cama acabou de ser ocupada.');
    expect(component.saving).toBe(false);
  });

  it('ignora camas recebidas para uma enfermaria que já não está seleccionada', () => {
    const earlierBeds = new Subject<BedResponse[]>();
    const selectedBeds = new Subject<BedResponse[]>();
    inpatientService.findBedsByWard.and.returnValues(earlierBeds, selectedBeds);

    component.form.controls.wardId.setValue('ward-old');
    component.form.controls.wardId.setValue('ward-new');
    selectedBeds.next([{ ...bed, id: 'bed-new' }]);
    earlierBeds.next([{ ...bed, id: 'bed-old' }]);

    expect(component.beds.map((item) => item.id)).toEqual(['bed-new']);
  });
});
