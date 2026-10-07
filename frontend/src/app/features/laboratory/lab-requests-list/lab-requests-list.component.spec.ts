import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LabRequestResponse, LabService } from '../../../core/services/lab.service';
import { LabRequestsListComponent } from './lab-requests-list.component';

describe('LabRequestsListComponent', () => {
  let component: LabRequestsListComponent;
  let labService: jasmine.SpyObj<LabService>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const request: LabRequestResponse = {
    id: 'request-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    episodeId: null,
    requestedById: null,
    requestedByName: null,
    status: 'PENDING',
    priority: 'NORMAL',
    clinicalNotes: null,
    collectedAt: null,
    completedAt: null,
    createdAt: '2026-08-01T10:00:00Z',
    items: [],
  };

  beforeEach(() => {
    labService = jasmine.createSpyObj<LabService>('LabService', [
      'findAllRequests',
      'collect',
      'startAnalysis',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    labService.findAllRequests.and.returnValue(
      of({ content: [request], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
    );
    labService.collect.and.returnValue(of({ ...request, status: 'COLLECTED' }));
    labService.startAnalysis.and.returnValue(of({ ...request, status: 'IN_ANALYSIS' }));
    authService.getCurrentUser.and.returnValue({
      id: 'technician-1',
      fullName: 'Técnica',
      username: 'tecnica',
      email: 'tecnica@example.ao',
      roles: ['LAB_TECHNICIAN'],
      mustChangePassword: false,
    });
    TestBed.configureTestingModule({
      imports: [LabRequestsListComponent],
      providers: [
        { provide: LabService, useValue: labService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(LabRequestsListComponent).componentInstance;
  });

  it('carrega pedidos e mostra acções compatíveis com o perfil de laboratório', () => {
    component.ngOnInit();

    expect(labService.findAllRequests).toHaveBeenCalled();
    expect(component.canViewList).toBeTrue();
    expect(component.canCreate).toBeFalse();
    expect(component.canAdvance('PENDING')).toBeTrue();
    expect(component.canAdvance('COLLECTED')).toBeTrue();
  });

  it('impede perfis sem permissão de avançar o estado', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'doctor-1',
      fullName: 'Médica',
      username: 'medica',
      email: 'medica@example.ao',
      roles: ['DOCTOR'],
      mustChangePassword: false,
    });
    component.ngOnInit();
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.advanceStatus(request, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(labService.collect).not.toHaveBeenCalled();
    expect(component.canCreate).toBeTrue();
    expect(component.canAdvance('PENDING')).toBeFalse();
  });

  it('apresenta o erro da API ao falhar o carregamento', () => {
    labService.findAllRequests.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'Não autorizado para consultar pedidos.' },
            status: 403,
          }),
      ),
    );

    component.loadRequests();

    expect(component.error).toBe('Não autorizado para consultar pedidos.');
    expect(component.loading).toBeFalse();
    expect(component.requests).toEqual([]);
  });

  it('actualiza o pedido após recolher a amostra', () => {
    component.ngOnInit();
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.advanceStatus(request, event);

    expect(labService.collect).toHaveBeenCalledWith(request.id);
    expect(component.requests[0].status).toBe('COLLECTED');
  });
});
