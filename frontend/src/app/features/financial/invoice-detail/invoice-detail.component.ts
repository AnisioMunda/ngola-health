import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
  private readonly destroyRef = inject(DestroyRef);

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
  savingIssue = false;
  showPaymentForm = false;

  // Void
  voidReason = '';
  showVoidForm = false;
  savingVoid = false;

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
    if (id) {
      this.loadInvoice(id);
    } else {
      this.loading = false;
      this.error = 'Documento não encontrado.';
    }
  }

  loadInvoice(id: string): void {
    this.loading = true;
    this.error = '';
    this.financialService
      .findById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (inv) => {
          this.invoice = inv;
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar documento.');
          this.loading = false;
        },
      });
  }

  issue(): void {
    if (!this.invoice || this.savingIssue) return;
    this.savingIssue = true;
    this.error = '';
    this.success = '';
    this.financialService
      .issue(this.invoice.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.invoice = updated;
          this.savingIssue = false;
          this.success =
            updated.agtStatus === 'PENDING'
              ? 'Documento emitido e submetido à AGT.'
              : 'Documento emitido.';
          setTimeout(() => (this.success = ''), 4000);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao emitir.');
          this.savingIssue = false;
        },
      });
  }

  registerPayment(): void {
    const invoice = this.invoice;
    const amount = this.paymentAmount;
    if (
      !invoice ||
      this.savingPayment ||
      amount === null ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      amount > invoice.balance
    ) {
      return;
    }
    this.savingPayment = true;
    this.error = '';
    this.success = '';
    this.financialService
      .registerPayment(invoice.id, {
        amount,
        paymentMethod: this.paymentMethod,
        reference: this.paymentReference || undefined,
        notes: this.paymentNotes || undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
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
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao registar pagamento.');
          this.savingPayment = false;
        },
      });
  }

  voidInvoice(): void {
    const reason = this.voidReason.trim();
    if (!this.invoice || this.savingVoid || !reason) return;
    this.savingVoid = true;
    this.error = '';
    this.success = '';
    this.financialService
      .void_(this.invoice.id, reason)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.invoice = updated;
          this.showVoidForm = false;
          this.voidReason = '';
          this.savingVoid = false;
          this.success = 'Documento anulado.';
          setTimeout(() => (this.success = ''), 3000);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao anular documento.');
          this.savingVoid = false;
        },
      });
  }

  downloadPdf(): void {
    if (!this.invoice) return;
    this.financialService
      .downloadPdf(this.invoice.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const filename =
            this.invoice!.invoiceNumber.replace(/ /g, '-').replace(/\//g, '-') + '.pdf';
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = filename;
          a.click();
          setTimeout(() => URL.revokeObjectURL(url));
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao gerar PDF.');
        },
      });
  }

  goBack(): void {
    this.router.navigate(['/financial']);
  }

  canRegisterPayment(): boolean {
    return (
      !!this.invoice &&
      (this.invoice.status === 'EMITIDO' ||
        this.invoice.status === 'PAGO_PARCIALMENTE' ||
        this.invoice.status === 'EM_ATRASO')
    );
  }

  canVoid(): boolean {
    return !!this.invoice && this.invoice.status !== 'PAGO' && this.invoice.status !== 'ANULADO';
  }

  isPaymentValid(): boolean {
    return (
      !!this.invoice &&
      this.paymentAmount !== null &&
      Number.isFinite(this.paymentAmount) &&
      this.paymentAmount > 0 &&
      this.hasCurrencyPrecision(this.paymentAmount) &&
      this.paymentAmount <= this.invoice.balance
    );
  }

  isPaymentAmountInvalid(): boolean {
    return (
      this.paymentAmount !== null &&
      !!this.invoice &&
      (!Number.isFinite(this.paymentAmount) ||
        this.paymentAmount <= 0 ||
        !this.hasCurrencyPrecision(this.paymentAmount) ||
        this.paymentAmount > this.invoice.balance)
    );
  }

  private hasCurrencyPrecision(amount: number): boolean {
    const cents = amount * 100;
    return Math.abs(cents - Math.round(cents)) < 1e-7;
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
