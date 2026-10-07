import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FinancialService, InvoiceResponse } from '../../../core/services/financial.service';
import { PatientService } from '../../../core/services/patient.service';
import { InvoiceFormComponent } from './invoice-form.component';

describe('InvoiceFormComponent', () => {
  let financialService: jasmine.SpyObj<FinancialService>;
  let patientService: jasmine.SpyObj<PatientService>;
  let router: jasmine.SpyObj<Router>;
  let component: InvoiceFormComponent;

  const invoice: InvoiceResponse = {
    id: 'invoice-1',
    invoiceNumber: 'FR 2026/0000001',
    currency: 'AOA',
    documentType: 'FR',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    patientNif: '',
    episodeId: '',
    status: 'RASCUNHO',
    paymentMethod: 'NUMERARIO',
    subtotal: 0.05,
    discountAmount: 0,
    vatAmount: 0,
    totalAmount: 0.05,
    paidAmount: 0,
    balance: 0.05,
    insuranceProvider: '',
    insurancePolicyNumber: '',
    insuranceCoveragePercent: 0,
    issuedAt: '',
    dueDate: '',
    paidAt: '',
    notes: '',
    createdAt: '2026-10-07T10:00:00Z',
    agtStatus: 'NAO_SUBMETIDO',
    agtValidationCode: '',
    items: [],
    payments: [],
  };

  beforeEach(() => {
    financialService = jasmine.createSpyObj<FinancialService>('FinancialService', [
      'findAllPrices',
      'create',
    ]);
    financialService.findAllPrices.and.returnValue(of([]));
    financialService.create.and.returnValue(of(invoice));
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    patientService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 200 }),
    );
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [InvoiceFormComponent],
      providers: [
        { provide: FinancialService, useValue: financialService },
        { provide: PatientService, useValue: patientService },
        { provide: Router, useValue: router },
      ],
    });

    const fixture = TestBed.createComponent(InvoiceFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('calcula a pré-visualização de desconto e IVA com HALF_UP a duas casas', () => {
    component.items.at(0).patchValue({
      unitPrice: 0.05,
      quantity: 1,
      discountPercent: 10,
      vatRate: 12.5,
    });

    expect(component.getLineTotal(0)).toBe(0.05);
    expect(component.getGrandTotal()).toBe(0.05);
  });

  it('valida o paciente e envia os campos fiscais e itens normalizados', () => {
    component.form.patchValue({
      patientId: 'patient-1',
      documentType: 'FT',
      insurancePolicyNumber: '',
      insuranceCoveragePercent: 25,
      dueDate: '',
    });
    component.items.at(0).patchValue({
      description: 'Consulta',
      quantity: 1,
      unitPrice: 100,
      discountPercent: 0,
      vatRate: 0,
      servicePriceId: '',
    });

    component.onSubmit();

    expect(financialService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        patientId: 'patient-1',
        documentType: 'FT',
        insurancePolicyNumber: undefined,
        insuranceCoveragePercent: 25,
        dueDate: undefined,
        items: [
          jasmine.objectContaining({
            description: 'Consulta',
            unitPrice: 100,
            servicePriceId: undefined,
          }),
        ],
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/financial']);
  });

  it('apresenta o detalhe do erro da API e liberta o estado de submissão', () => {
    financialService.create.and.returnValue(
      throwError(() => ({ error: { detail: 'O preço indicado é inválido.' } })),
    );
    component.form.patchValue({ patientId: 'patient-1' });
    component.items.at(0).patchValue({ description: 'Consulta', unitPrice: 10 });

    component.onSubmit();

    expect(component.error).toBe('O preço indicado é inválido.');
    expect(component.saving).toBeFalse();
  });
});
