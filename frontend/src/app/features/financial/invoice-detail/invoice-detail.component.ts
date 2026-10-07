import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  FinancialService,
  InvoiceResponse,
  PaymentMethod,
  DOCUMENT_TYPE_LABELS,
  STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
} from '../../../core/services/financial.service';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './invoice-detail.component.html',
  styleUrls: ['./invoice-detail.component.scss'],
})
export class InvoiceDetailComponent implements OnInit {
  invoice: InvoiceResponse | null = null;
  loading = true;
  error = '';
  success = '';

  // Payment form
  paymentAmount: number | null = null;
  paymentMethod: PaymentMethod = 'NUMERARIO';
  paymentReference = '';
  paymentNotes = '';
  savingPayment = false;
  showPaymentForm = false;

  // Void
  voidReason = '';
  showVoidForm = false;

  docLabels = DOCUMENT_TYPE_LABELS;
  statusLabels = STATUS_LABELS;
  pmLabels = PAYMENT_METHOD_LABELS;

  paymentMethods: { value: PaymentMethod; label: string }[] = [
    { value: 'NUMERARIO', label: 'Numerário' },
    { value: 'TRANSFERENCIA', label: 'Transferência Bancária' },
    { value: 'CARTAO', label: 'Cartão' },
    { value: 'SEGURO', label: 'Seguro de Saúde' },
    { value: 'CHEQUE', label: 'Cheque' },
    { value: 'DINHEIRO_MOVEL', label: 'Dinheiro Móvel' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private financialService: FinancialService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadInvoice(id);
  }

  loadInvoice(id: string): void {
    this.loading = true;
    this.financialService.findById(id).subscribe({
      next: (inv) => {
        this.invoice = inv;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar documento.';
        this.loading = false;
      },
    });
  }

  issue(): void {
    if (!this.invoice) return;
    this.financialService.issue(this.invoice.id).subscribe({
      next: (updated) => {
        this.invoice = updated;
        this.success = 'Documento emitido e submetido à AGT.';
        setTimeout(() => (this.success = ''), 4000);
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao emitir.';
      },
    });
  }

  registerPayment(): void {
    if (!this.invoice || !this.paymentAmount) return;
    this.savingPayment = true;
    this.financialService
      .registerPayment(this.invoice.id, {
        amount: this.paymentAmount,
        paymentMethod: this.paymentMethod,
        reference: this.paymentReference || undefined,
        notes: this.paymentNotes || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.invoice = updated;
          this.showPaymentForm = false;
          this.paymentAmount = null;
          this.paymentReference = '';
          this.paymentNotes = '';
          this.savingPayment = false;
          this.success = 'Pagamento registado com sucesso.';
          setTimeout(() => (this.success = ''), 3000);
        },
        error: (err) => {
          this.error = err.error?.message ?? 'Erro ao registar pagamento.';
          this.savingPayment = false;
        },
      });
  }

  voidInvoice(): void {
    if (!this.invoice || !this.voidReason) return;
    this.financialService.void_(this.invoice.id, this.voidReason).subscribe({
      next: (updated) => {
        this.invoice = updated;
        this.showVoidForm = false;
        this.voidReason = '';
        this.success = 'Documento anulado.';
        setTimeout(() => (this.success = ''), 3000);
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao anular documento.';
      },
    });
  }

  downloadPdf(): void {
    if (!this.invoice) return;
    this.financialService.downloadPdf(this.invoice.id).subscribe({
      next: (blob) => {
        const filename =
          this.invoice!.invoiceNumber.replace(/ /g, '-').replace(/\//g, '-') + '.pdf';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'Erro ao gerar PDF.';
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/financial']);
  }

  canRegisterPayment(): boolean {
    return (
      !!this.invoice &&
      (this.invoice.status === 'EMITIDO' || this.invoice.status === 'PAGO_PARCIALMENTE')
    );
  }

  canVoid(): boolean {
    return !!this.invoice && this.invoice.status !== 'PAGO' && this.invoice.status !== 'ANULADO';
  }
}
