import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ReportService } from '../../../core/services/report.service';
import { MedicationResponse, PharmacyService } from '../../../core/services/pharmacy.service';
import { MedicationsListComponent } from './medications-list.component';

describe('MedicationsListComponent', () => {
  let component: MedicationsListComponent;
  let pharmacyService: jasmine.SpyObj<PharmacyService>;
  let reportService: jasmine.SpyObj<ReportService>;
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;

  const medication: MedicationResponse = {
    id: 'med-1',
    name: 'Amoxicilina',
    genericName: 'Amoxicilina',
    dosageForm: 'CAPSULE',
    strength: '500 mg',
    unit: 'cápsula',
    requiresPrescription: true,
    minStockLevel: 10,
    totalAvailable: 4,
    active: true,
  };

  beforeEach(() => {
    pharmacyService = jasmine.createSpyObj<PharmacyService>('PharmacyService', [
      'findAllMedications',
    ]);
    reportService = jasmine.createSpyObj<ReportService>('ReportService', [
      'downloadStockReport',
      'openOrDownload',
    ]);
    reportService.downloadStockReport.and.returnValue(
      of(new Blob(['pdf'], { type: 'application/pdf' })),
    );
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    pharmacyService.findAllMedications.and.returnValue(
      of({ content: [medication], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
    );
    authService.getCurrentUser.and.returnValue({
      id: 'pharmacist-1',
      fullName: 'Farmacêutica',
      username: 'farmaceutica',
      email: 'farmacia@example.ao',
      roles: ['PHARMACIST'],
      mustChangePassword: false,
    });

    TestBed.configureTestingModule({
      imports: [MedicationsListComponent],
      providers: [
        { provide: PharmacyService, useValue: pharmacyService },
        { provide: ReportService, useValue: reportService },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
      ],
    });
    component = TestBed.createComponent(MedicationsListComponent).componentInstance;
    component.ngOnInit();
  });

  afterEach(() => component.ngOnDestroy());

  it('carrega medicamentos e apresenta a forma farmacêutica em português', () => {
    expect(component.medications).toEqual([medication]);
    expect(component.formLabels.CAPSULE).toBe('Cápsula');
    expect(component.isLowStock(medication)).toBe(true);
  });

  it('permite operações de stock apenas a perfis autorizados', () => {
    expect(component.canManagePharmacy).toBe(true);
    expect(component.canViewStockDetails).toBe(true);

    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestor',
      username: 'gestor',
      email: 'gestor@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });
    expect(component.canManagePharmacy).toBe(false);
    expect(component.canViewStockDetails).toBe(true);
  });

  it('impede o acesso do médico aos detalhes de stock', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });

    component.goToMedication(medication.id);
    component.goToExpiring();

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('descarrega o inventário PDF apenas para perfis com acesso ao stock', () => {
    component.downloadStockReport();

    expect(reportService.downloadStockReport).toHaveBeenCalled();
    expect(reportService.openOrDownload).toHaveBeenCalledWith(
      jasmine.any(Blob),
      'inventario-stock.pdf',
    );
    expect(component.downloadingReport).toBe(false);

    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });
    reportService.downloadStockReport.calls.reset();
    component.downloadStockReport();

    expect(reportService.downloadStockReport).not.toHaveBeenCalled();
  });

  it('apresenta falhas ao gerar o inventário PDF', () => {
    reportService.downloadStockReport.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Inventário temporariamente indisponível.' },
            status: 503,
          }),
      ),
    );

    component.downloadStockReport();

    expect(component.reportError).toBe('Inventário temporariamente indisponível.');
    expect(component.downloadingReport).toBe(false);
    expect(reportService.openOrDownload).not.toHaveBeenCalled();
  });

  it('apresenta o detalhe RFC 7807 quando a pesquisa falha', () => {
    pharmacyService.findAllMedications.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Catálogo temporariamente indisponível.' },
            status: 503,
          }),
      ),
    );

    component.loadMedications();

    expect(component.error).toBe('Catálogo temporariamente indisponível.');
    expect(component.medications).toEqual([]);
    expect(component.loading).toBe(false);
  });
});
