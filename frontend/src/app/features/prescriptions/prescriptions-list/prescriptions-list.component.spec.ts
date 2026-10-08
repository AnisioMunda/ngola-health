import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { PrescriptionsListComponent } from './prescriptions-list.component';

describe('PrescriptionsListComponent', () => {
  let component: PrescriptionsListComponent;
  let prescriptionService: jasmine.SpyObj<PrescriptionService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    prescriptionService = jasmine.createSpyObj<PrescriptionService>('PrescriptionService', [
      'getStats',
      'findAll',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    prescriptionService.getStats.and.returnValue(
      of({ totalActive: 1, totalToday: 1, pendingDispense: 0, expiringSoon: 0 }),
    );
    prescriptionService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
    );
    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });

    TestBed.configureTestingModule({
      imports: [PrescriptionsListComponent],
      providers: [
        provideRouter([]),
        { provide: PrescriptionService, useValue: prescriptionService },
        { provide: AuthService, useValue: authService },
      ],
    });
    router = TestBed.inject(Router);
    component = TestBed.createComponent(PrescriptionsListComponent).componentInstance;
  });

  it('carrega a lista e permite criar a perfis clínicos autorizados', () => {
    component.ngOnInit();

    expect(prescriptionService.getStats).toHaveBeenCalled();
    expect(prescriptionService.findAll).toHaveBeenCalled();
    expect(component.canCreate).toBe(true);
  });

  it('não consulta nem permite criar para um perfil sem acesso à lista', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'nurse-1',
      fullName: 'Enfermeira',
      username: 'enfermeira',
      email: 'enfermeira@example.ao',
      roles: ['NURSE'],
      mustChangePassword: false,
    });

    component.ngOnInit();
    component.goToNew();

    expect(component.error).toContain('não tem permissão');
    expect(component.canCreate).toBe(false);
    expect(prescriptionService.getStats).not.toHaveBeenCalled();
    expect(prescriptionService.findAll).not.toHaveBeenCalled();
    expect(router.url).toBe('/');
  });

  it('mostra a mensagem da API quando falha o carregamento', () => {
    prescriptionService.findAll.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não autorizado para consultar prescrições.' },
            status: 403,
          }),
      ),
    );

    component.load();

    expect(component.error).toBe('Não autorizado para consultar prescrições.');
    expect(component.loading).toBe(false);
  });

  it('rejeita um intervalo de datas invertido sem chamar a API', () => {
    component.dateFrom = '2026-08-02';
    component.dateTo = '2026-08-01';

    component.load();

    expect(component.error).toBe('A data inicial não pode ser posterior à data final.');
    expect(prescriptionService.findAll).not.toHaveBeenCalled();
  });
});
