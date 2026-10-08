import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import {
  BedResponse,
  CreateWardRequest,
  InpatientService,
  WardResponse,
} from '../../../core/services/inpatient.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { WardSetupComponent } from './ward-setup.component';

describe('WardSetupComponent', () => {
  let inpatientService: jasmine.SpyObj<InpatientService>;
  let fixture: ComponentFixture<WardSetupComponent>;
  let component: WardSetupComponent;

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

  beforeEach(() => {
    inpatientService = jasmine.createSpyObj<InpatientService>('InpatientService', [
      'findAllWards',
      'findBedsByWard',
      'createWard',
      'createBed',
      'updateBedStatus',
    ]);
    inpatientService.findAllWards.and.returnValue(of([ward]));
    inpatientService.findBedsByWard.and.returnValue(of([bed]));
    inpatientService.createWard.and.returnValue(of(ward));
    inpatientService.createBed.and.returnValue(of(bed));
    inpatientService.updateBedStatus.and.returnValue(of(bed));

    TestBed.configureTestingModule({
      imports: [WardSetupComponent],
      providers: [
        { provide: InpatientService, useValue: inpatientService },
        {
          provide: UserManagementService,
          useValue: {
            findAll: () =>
              of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
          },
        },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
      ],
    });

    fixture = TestBed.createComponent(WardSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega as enfermarias e apresenta um selector acessível', () => {
    expect(component.wards).toEqual([ward]);
    expect(component.beds).toEqual([bed]);
    expect(component.selectedWardId).toBe(ward.id);
    expect(component.loading).toBe(false);
    const wardButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.ward-item',
    );
    expect(wardButton?.getAttribute('aria-pressed')).toBe('true');
  });

  it('selecciona a nova enfermaria após a criação', () => {
    const createdWard = { ...ward, id: 'ward-2', name: 'Pediatria', code: 'PED' };
    inpatientService.createWard.and.returnValue(of(createdWard));
    inpatientService.findAllWards.and.returnValue(of([ward, createdWard]));
    inpatientService.findBedsByWard.and.returnValue(of([]));
    component.wardForm.patchValue({ name: 'Pediatria', code: 'ped' });

    component.createWard();

    const request: CreateWardRequest = inpatientService.createWard.calls.mostRecent().args[0];
    expect(request.code).toBe('PED');
    expect(component.selectedWardId).toBe(createdWard.id);
    expect(inpatientService.findBedsByWard).toHaveBeenCalledWith(createdWard.id);
    expect(component.success).toBe('Enfermaria criada com sucesso.');
    expect(component.wardForm.getRawValue()).toEqual({
      name: '',
      code: '',
      type: 'GENERAL',
      floor: '',
      notes: '',
      responsibleDoctorId: '',
    });
  });

  it('não envia formulário inválido e apresenta detalhes devolvidos pela API', () => {
    component.wardForm.patchValue({ name: '', code: '' });
    component.createWard();
    expect(inpatientService.createWard).not.toHaveBeenCalled();

    inpatientService.createWard.and.returnValue(
      throwError(() => ({ error: { detail: 'O código já está em uso.' } })),
    );
    component.wardForm.patchValue({ name: 'Pediatria', code: 'PED' });
    component.createWard();

    expect(component.error).toBe('O código já está em uso.');
    expect(component.savingWard).toBe(false);
  });

  it('ignora a resposta de camas de uma enfermaria já não seleccionada', () => {
    const earlierBeds = new Subject<BedResponse[]>();
    const selectedBeds = new Subject<BedResponse[]>();
    inpatientService.findBedsByWard.and.returnValues(earlierBeds, selectedBeds);

    component.selectedWardId = 'ward-old';
    component.loadBeds();
    component.selectedWardId = 'ward-new';
    component.loadBeds();
    selectedBeds.next([{ ...bed, id: 'bed-new' }]);
    earlierBeds.next([{ ...bed, id: 'bed-old' }]);

    expect(component.beds.map((item) => item.id)).toEqual(['bed-new']);
  });
});
