import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import {
  BedResponse,
  InpatientService,
  WardMapResponse,
  WardResponse,
} from '../../../core/services/inpatient.service';
import { WardMapComponent } from './ward-map.component';

describe('WardMapComponent', () => {
  let inpatientService: jasmine.SpyObj<InpatientService>;
  let router: jasmine.SpyObj<Router>;
  let fixture: ComponentFixture<WardMapComponent>;
  let component: WardMapComponent;

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

  const wardMap: WardMapResponse = {
    wardId: ward.id,
    wardName: ward.name,
    wardType: 'GENERAL',
    totalBeds: 1,
    availableBeds: 1,
    occupiedBeds: 0,
    maintenanceBeds: 0,
    beds: [bed],
  };

  beforeEach(() => {
    inpatientService = jasmine.createSpyObj<InpatientService>('InpatientService', [
      'findAllWards',
      'getWardMap',
      'updateBedStatus',
    ]);
    inpatientService.findAllWards.and.returnValue(of([ward]));
    inpatientService.getWardMap.and.returnValue(of(wardMap));
    inpatientService.updateBedStatus.and.returnValue(of(bed));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [WardMapComponent],
      providers: [
        { provide: InpatientService, useValue: inpatientService },
        { provide: Router, useValue: router },
      ],
    });

    fixture = TestBed.createComponent(WardMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega o mapa e apresenta camas com rótulos acessíveis', () => {
    expect(component.wardMap).toEqual(wardMap);
    expect(component.getBedAriaLabel(bed)).toBe('Cama 01, Disponível');
    const bedButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.bed-card',
    );
    expect(bedButton?.getAttribute('aria-label')).toBe('Cama 01, Disponível');
  });

  it('ignora a resposta de um mapa de uma enfermaria não seleccionada', () => {
    const earlierMap = new Subject<WardMapResponse>();
    const selectedMap = new Subject<WardMapResponse>();
    inpatientService.getWardMap.and.returnValues(earlierMap, selectedMap);

    component.selectedWardId = 'ward-old';
    component.loadMap();
    component.selectedWardId = ward.id;
    component.loadMap();
    selectedMap.next(wardMap);
    earlierMap.next({ ...wardMap, wardId: 'ward-old', wardName: 'Antiga' });

    expect(component.wardMap).toEqual(wardMap);
    expect(component.loadingMap).toBeFalse();
  });

  it('apresenta detalhes de erro ao carregar o mapa', () => {
    inpatientService.getWardMap.and.returnValue(
      throwError(() => ({ error: { detail: 'Enfermaria inactiva.' } })),
    );

    component.loadMap();

    expect(component.error).toBe('Enfermaria inactiva.');
    expect(component.loadingMap).toBeFalse();
    expect(component.wardMap).toBeNull();
  });

  it('actualiza o estado da cama e recarrega o mapa', () => {
    component.selectBed({ ...bed, status: 'MAINTENANCE' });
    component.setBedStatus('AVAILABLE');

    expect(inpatientService.updateBedStatus).toHaveBeenCalledWith('bed-1', 'AVAILABLE');
    expect(component.showStatusModal).toBeFalse();
    expect(component.selectedBed).toBeNull();
    expect(component.success).toBe('Estado da cama actualizado.');
    expect(inpatientService.getWardMap).toHaveBeenCalledTimes(2);
  });

  it('navega para o internamento associado à cama ocupada', () => {
    component.selectBed({
      ...bed,
      status: 'OCCUPIED',
      admissionId: 'admission-1',
      patientName: 'Ana Silva',
    });

    expect(router.navigate).toHaveBeenCalledWith(['/inpatient/admissions', 'admission-1']);
    expect(
      component.getBedAriaLabel({ ...bed, status: 'OCCUPIED', patientName: 'Ana Silva' }),
    ).toBe('Cama 01, Ocupada, paciente Ana Silva');
  });

  it('envia a enfermaria e a cama ao iniciar uma admissão pelo mapa', () => {
    component.selectBed(bed);
    component.goToAdmit();

    expect(router.navigate).toHaveBeenCalledWith(['/inpatient/admissions/new'], {
      queryParams: { bedId: bed.id, wardId: ward.id },
    });
  });
});
