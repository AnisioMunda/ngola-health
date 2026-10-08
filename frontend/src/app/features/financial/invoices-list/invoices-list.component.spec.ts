import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { FinancialService, InvoiceResponse, Page } from '../../../core/services/financial.service';
import { InvoicesListComponent } from './invoices-list.component';

describe('InvoicesListComponent', () => {
  let financialService: jasmine.SpyObj<FinancialService>;
  let router: jasmine.SpyObj<Router>;
  let component: InvoicesListComponent;

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
    subtotal: 100,
    discountAmount: 0,
    vatAmount: 0,
    totalAmount: 100,
    paidAmount: 0,
    balance: 100,
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
  const page: Page<InvoiceResponse> = {
    content: [invoice],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  };

  beforeEach(() => {
    financialService = jasmine.createSpyObj<FinancialService>('FinancialService', [
      'findAllInvoices',
      'issue',
      'downloadPdf',
    ]);
    financialService.findAllInvoices.and.returnValue(of(page));
    financialService.issue.and.returnValue(of({ ...invoice, status: 'EMITIDO' }));
    financialService.downloadPdf.and.returnValue(of(new Blob()));
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [InvoicesListComponent],
      providers: [
        { provide: FinancialService, useValue: financialService },
        { provide: Router, useValue: router },
      ],
    });

    const fixture = TestBed.createComponent(InvoicesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega facturas, filtra por estado e navega pelo número acessível', () => {
    expect(component.invoices).toEqual([invoice]);

    component.statusFilter = 'PAGO';
    component.onFilterChange();

    expect(financialService.findAllInvoices.calls.mostRecent().args).toEqual([
      undefined,
      'PAGO',
      0,
      20,
    ]);
    component.goToDetail(invoice.id);
    expect(router.navigate).toHaveBeenCalledWith(['/financial', invoice.id]);
  });

  it('actualiza o estado após emissão e impede pedidos repetidos', () => {
    const pendingIssue = new Subject<InvoiceResponse>();
    financialService.issue.and.returnValue(pendingIssue);
    const event = new Event('click');

    component.issue(invoice.id, event);
    component.issue(invoice.id, event);
    expect(financialService.issue).toHaveBeenCalledTimes(1);

    pendingIssue.next({ ...invoice, status: 'EMITIDO', agtStatus: 'PENDING' });

    expect(component.invoices[0].status).toBe('EMITIDO');
    expect(component.issuingInvoiceIds.has(invoice.id)).toBe(false);
  });

  it('mostra o detalhe do erro devolvido pela API', () => {
    financialService.findAllInvoices.and.returnValue(
      throwError(() => ({ error: { detail: 'Sem acesso às facturas.' } })),
    );

    component.loadInvoices();

    expect(component.error).toBe('Sem acesso às facturas.');
    expect(component.loading).toBe(false);
  });
});
