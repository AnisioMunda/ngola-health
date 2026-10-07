import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LabService, LabRequestResponse } from '../../../core/services/lab.service';
import { LabRequestDetailComponent } from './lab-request-detail.component';

describe('LabRequestDetailComponent', () => {
  let component: LabRequestDetailComponent;
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
    status: 'IN_ANALYSIS',
    priority: 'NORMAL',
    clinicalNotes: null,
    collectedAt: '2026-08-01T10:00:00Z',
    completedAt: null,
    createdAt: '2026-08-01T09:00:00Z',
    items: [
      {
        id: 'item-1',
        labTestId: 'test-1',
        testCode: 'HEM-01',
        testName: 'Hemograma',
        resultValue: null,
        resultUnit: null,
        referenceRange: null,
        abnormal: false,
        resultNotes: null,
        resultedAt: null,
      },
    ],
  };

  beforeEach(() => {
    labService = jasmine.createSpyObj<LabService>('LabService', [
      'findRequestById',
      'collect',
      'startAnalysis',
      'submitResult',
      'cancel',
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    labService.findRequestById.and.returnValue(of(request));
    labService.submitResult.and.returnValue(of(request));
    labService.collect.and.returnValue(of(request));
    labService.startAnalysis.and.returnValue(of(request));
    labService.cancel.and.returnValue(of(request));
    authService.getCurrentUser.and.returnValue({
      id: 'technician-1',
      fullName: 'Técnica',
      username: 'tecnica',
      email: 'tecnica@example.ao',
      roles: ['LAB_TECHNICIAN'],
      mustChangePassword: false,
    });
    TestBed.configureTestingModule({
      imports: [LabRequestDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'request-1' }) } },
        },
        { provide: LabService, useValue: labService },
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(LabRequestDetailComponent).componentInstance;
  });

  it('apresenta operações de análise permitidas à equipa de laboratório', () => {
    component.ngOnInit();

    expect(component.canSubmitResults).toBeTrue();
    expect(component.canCollect).toBeFalse();
    expect(component.canStartAnalysis).toBeFalse();
    expect(component.canCancel).toBeTrue();
  });

  it('regista resultado uma vez e actualiza o pedido', () => {
    component.ngOnInit();
    const item = request.items[0];
    component.startEditing(item);
    component.resultForm.resultValue = '  Normal  ';
    component.resultForm.resultUnit = ' g/dL ';

    component.saveResult(item);

    expect(labService.submitResult).toHaveBeenCalledWith(request.id, item.id, {
      resultValue: 'Normal',
      resultUnit: 'g/dL',
      referenceRange: undefined,
      abnormal: false,
      resultNotes: undefined,
    });
    expect(component.saving).toBeFalse();
    expect(component.editingItemId).toBeNull();
  });

  it('não permite inserir nem alterar resultados fora da fase autorizada', () => {
    authService.getCurrentUser.and.returnValue({
      id: 'manager-1',
      fullName: 'Gestora',
      username: 'gestora',
      email: 'gestora@example.ao',
      roles: ['MANAGER'],
      mustChangePassword: false,
    });
    component.ngOnInit();

    component.startEditing(request.items[0]);

    expect(component.canSubmitResults).toBeFalse();
    expect(component.editingItemId).toBeNull();
    expect(labService.submitResult).not.toHaveBeenCalled();
  });

  it('mostra erros de API ao tentar cancelar o pedido', () => {
    labService.cancel.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            error: { detail: 'O pedido já foi concluído.' },
            status: 409,
          }),
      ),
    );
    component.ngOnInit();

    component.cancelRequest();

    expect(component.error).toBe('O pedido já foi concluído.');
    expect(component.saving).toBeFalse();
  });
});
