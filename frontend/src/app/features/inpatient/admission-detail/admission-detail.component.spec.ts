import { registerLocaleData } from '@angular/common';
import localePtAo from '@angular/common/locales/pt-AO';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import {
  AdmissionResponse,
  BedResponse,
  InpatientService,
  WardResponse,
} from '../../../core/services/inpatient.service';
import { AdmissionDetailComponent } from './admission-detail.component';

registerLocaleData(localePtAo);

describe('AdmissionDetailComponent', () => {
  let inpatientService: jasmine.SpyObj<InpatientService>;
  let router: jasmine.SpyObj<Router>;
  let component: AdmissionDetailComponent;

  const ward: WardResponse = {
    id: 'ward-1',
    name: 'Medicina Geral',
    code: 'MG',
    type: 'GENERAL',
    typeLabel: 'Medicina Geral',
    floor: null,
    totalBeds: 1,
    availableBeds: 0,
    occupiedBeds: 1,
    responsibleDoctorName: null,
    notes: null,
    active: true,
  };

  const destinationWard: WardResponse = {
    ...ward,
    id: 'ward-2',
    name: 'Pediatria',
    code: 'PED',
    availableBeds: 1,
    occupiedBeds: 0,
  };

  const bed: BedResponse = {
    id: 'bed-1',
    bedNumber: '01',
    status: 'OCCUPIED',
    statusLabel: 'Ocupada',
    type: 'STANDARD',
    wardName: ward.name,
    wardId: ward.id,
    notes: null,
    patientName: 'Ana Silva',
    admissionId: 'admission-1',
    admissionDate: '2026-10-07T10:00:00Z',
  };

  const destinationBed: BedResponse = {
    ...bed,
    id: 'bed-2',
    bedNumber: '02',
    status: 'AVAILABLE',
    statusLabel: 'Disponível',
    wardName: destinationWard.name,
    wardId: destinationWard.id,
    patientName: null,
    admissionId: null,
    admissionDate: null,
  };

  const admission: AdmissionResponse = {
    id: 'admission-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    patientPhone: '923000000',
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
    diagnosis: null,
    dischargeNotes: null,
    dischargeCondition: null,
    daysAdmitted: 1,
    admittedByName: 'Admin',
    dischargedByName: null,
    createdAt: '2026-10-07T10:00:00Z',
    transfers: [],
  };

  beforeEach(() => {
    inpatientService = jasmine.createSpyObj<InpatientService>('InpatientService', [
      'findById',
      'findAllWards',
      'findBedsByWard',
      'discharge',
      'transfer',
    ]);
    inpatientService.findById.and.returnValue(of(admission));
    inpatientService.findAllWards.and.returnValue(of([ward, destinationWard]));
    inpatientService.findBedsByWard.and.returnValue(of([]));
    inpatientService.discharge.and.returnValue(of(admission));
    inpatientService.transfer.and.returnValue(of(admission));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [AdmissionDetailComponent],
      providers: [
        { provide: InpatientService, useValue: inpatientService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: admission.id }) } },
        },
        { provide: Router, useValue: router },
      ],
    });
    const fixture = TestBed.createComponent(AdmissionDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega o internamento e só permite operações enquanto está activo', () => {
    expect(inpatientService.findById).toHaveBeenCalledWith(admission.id);
    expect(component.admission).toEqual(admission);
    expect(component.canDischarge()).toBe(true);
    expect(component.canTransfer()).toBe(true);
  });

  it('regista a alta com notas normalizadas e actualiza o detalhe', () => {
    const discharged = {
      ...admission,
      status: 'DISCHARGED',
      dischargeDate: '2026-10-08T10:00:00Z',
      dischargeNotes: 'Repouso e hidratação',
      dischargeCondition: 'IMPROVED',
    } satisfies AdmissionResponse;
    inpatientService.discharge.and.returnValue(of(discharged));
    component.openDischargeForm();
    component.dischargeNotes = '  Repouso e hidratação  ';

    component.discharge();

    expect(inpatientService.discharge).toHaveBeenCalledWith(admission.id, {
      dischargeNotes: 'Repouso e hidratação',
      dischargeCondition: 'IMPROVED',
    });
    expect(component.admission).toEqual(discharged);
    expect(component.showDischargeForm).toBe(false);
    expect(component.canDischarge()).toBe(false);
    expect(component.success).toBe('Alta registada com sucesso.');
  });

  it('apresenta o conflito da API sem perder o internamento activo', () => {
    inpatientService.discharge.and.returnValue(
      throwError(() => ({ error: { detail: 'O internamento já recebeu alta.' } })),
    );
    component.discharge();

    expect(component.error).toBe('O internamento já recebeu alta.');
    expect(component.savingDischarge).toBe(false);
    expect(component.admission).toEqual(admission);
  });

  it('permite transferir para camas disponíveis noutra enfermaria', () => {
    inpatientService.findBedsByWard.and.callFake((wardId) =>
      of(wardId === destinationWard.id ? [destinationBed] : [bed]),
    );
    const transferred = {
      ...admission,
      bedId: destinationBed.id,
      bedNumber: destinationBed.bedNumber,
      wardId: destinationWard.id,
      wardName: destinationWard.name,
    };
    inpatientService.transfer.and.returnValue(of(transferred));

    component.openTransferForm();
    component.selectedWardId = destinationWard.id;
    component.onTransferWardChange();
    component.selectedBedId = destinationBed.id;
    component.transferReason = '  Continuação de cuidados  ';
    component.transfer();

    expect(inpatientService.findBedsByWard).toHaveBeenCalledWith(destinationWard.id);
    expect(inpatientService.transfer).toHaveBeenCalledWith(admission.id, {
      toBedId: destinationBed.id,
      reason: 'Continuação de cuidados',
    });
    expect(component.admission).toEqual(transferred);
    expect(component.showTransferForm).toBe(false);
  });

  it('ignora camas recebidas para uma enfermaria já não seleccionada', () => {
    const earlierBeds = new Subject<BedResponse[]>();
    const selectedBeds = new Subject<BedResponse[]>();
    inpatientService.findBedsByWard.and.returnValues(earlierBeds, selectedBeds);
    component.admission = admission;

    component.loadAvailableBeds('ward-old');
    component.loadAvailableBeds(destinationWard.id);
    selectedBeds.next([destinationBed]);
    earlierBeds.next([{ ...destinationBed, id: 'bed-old' }]);

    expect(component.availableBeds.map((item) => item.id)).toEqual([destinationBed.id]);
  });
});
