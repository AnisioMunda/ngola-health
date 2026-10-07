import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FinancialService, InvoiceResponse } from '../../../core/services/financial.service';
import { InvoiceDetailComponent } from './invoice-detail.component';

describe('InvoiceDetailComponent', () => {
  let financialService: jasmine.SpyObj<FinancialService>;
  let router: jasmine.SpyObj<Router>;
  let component: InvoiceDetailComponent;

  const invoice: InvoiceResponse = {
    id: 'invoice-1',
    invoiceNumber: 'FR 2026/0000001',
    currency: 'AOA',
    documentType: 'FR',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    patientNif: '',
    episodeId: '',
    status: 'EMITIDO',
    paymentMethod: 'NUMERARIO',
    subtotal: 100,
    discountAmount: 0,
    vatAmount: 0,
    totalAmount: 100,
    paidAmount: 0,
    balance: 100,
    insuranceProvider: '',
    insurancePolicyNumber: '',
    insuranceCoveragePercent: 0,
    issuedAt: '2026-10-07T10:00:00Z',
    dueDate: '',
    paidAt: '',
    notes: '',
    createdAt: '2026-10-07T10:00:00Z',
    agtStatus: 'PENDING',
    agtValidationCode: '',
    items: [],
    payments: [],
  };

  beforeEach(() => {
    financialService = jasmine.createSpyObj<FinancialService>('FinancialService', [
      'findById',
      'issue',
      'registerPayment',
      'void_',
      'downloadPdf',
    ]);
    financialService.findById.and.returnValue(of(invoice));
    financialService.issue.and.returnValue(of({ ...invoice, status: 'EMITIDO' }));
    financialService.registerPayment.and.returnValue(
      of({
        ...invoice,
        status: 'PAGO',
        paidAmount: 100,
        balance: 0,
        payments: [
          {
            id: 'payment-1',
            amount: 100,
            paymentMethod: 'NUMERARIO',
            reference: '',
            paidAt: '2026-10-07T10:00:00Z',
            receivedByName: '',
            notes: '',
          },
        ],
      }),
    );
    financialService.void_.and.returnValue(of({ ...invoice, status: 'ANULADO' }));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [InvoiceDetailComponent],
      providers: [
        { provide: FinancialService, useValue: financialService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: invoice.id }) } },
        },
        { provide: Router, useValue: router },
      ],
    });

    const fixture = TestBed.createComponent(InvoiceDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega a factura e permite pagamentos enquanto há saldo', () => {
    expect(financialService.findById).toHaveBeenCalledWith(invoice.id);
    expect(component.invoice).toEqual(invoice);
    expect(component.canRegisterPayment()).toBeTrue();
    expect(component.isPaymentValid()).toBeFalse();
  });

  it('regista pagamentos dentro do saldo e actualiza o detalhe', () => {
    component.paymentAmount = 100;
    component.registerPayment();

    expect(financialService.registerPayment).toHaveBeenCalledWith(invoice.id, {
      amount: 100,
      paymentMethod: 'NUMERARIO',
      reference: undefined,
      notes: undefined,
    });
    expect(component.invoice?.status).toBe('PAGO');
    expect(component.showPaymentForm).toBeFalse();
    expect(component.savingPayment).toBeFalse();
  });

  it('impede pagamentos superiores ao saldo e apresenta conflitos da API', () => {
    component.paymentAmount = 0.005;
    expect(component.isPaymentValid()).toBeFalse();
    component.paymentAmount = 100.01;
    expect(component.isPaymentValid()).toBeFalse();
    component.registerPayment();
    expect(financialService.registerPayment).not.toHaveBeenCalled();

    financialService.issue.and.returnValue(
      throwError(() => ({ error: { detail: 'O documento já foi emitido.' } })),
    );
    component.issue();
    expect(component.error).toBe('O documento já foi emitido.');
    expect(component.savingIssue).toBeFalse();
  });

  it('não permite motivo de anulação em branco', () => {
    component.voidReason = '   ';
    component.voidInvoice();

    expect(financialService.void_).not.toHaveBeenCalled();
  });
});
