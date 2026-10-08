import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import {
  FinancialService,
  InvoiceResponse,
  InvoiceStatus,
  DOCUMENT_TYPE_LABELS,
  STATUS_LABELS,
} from '../../../core/services/financial.service';

@Component({
  selector: 'app-invoices-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './invoices-list.component.html',
  styleUrls: ['./invoices-list.component.scss'],
})
export class InvoicesListComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private loadSubscription?: Subscription;

  invoices: InvoiceResponse[] = [];
  issuingInvoiceIds = new Set<string>();
  loading = true;
  error = '';
  statusFilter: InvoiceStatus | '' = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  docLabels = DOCUMENT_TYPE_LABELS;
  statusLabels = STATUS_LABELS;

  statuses: { value: InvoiceStatus | ''; label: string }[] = [
    { value: '', label: 'Todos os estados' },
    { value: 'RASCUNHO', label: 'Rascunho' },
    { value: 'EMITIDO', label: 'Emitido' },
    { value: 'PAGO_PARCIALMENTE', label: 'Pago Parcialmente' },
    { value: 'PAGO', label: 'Pago' },
    { value: 'ANULADO', label: 'Anulado' },
    { value: 'EM_ATRASO', label: 'Em Atraso' },
  ];

  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private financialService: FinancialService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loadSubscription?.unsubscribe();
    this.loading = true;
    this.error = '';
    this.loadSubscription = this.financialService
      .findAllInvoices(undefined, this.statusFilter || undefined, this.currentPage, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.invoices = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar documentos.');
          this.loading = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadInvoices();
  }

  goToCreate(): void {
    this.router.navigate(['/financial/new']);
  }
  goToDetail(id: string): void {
    this.router.navigate(['/financial', id]);
  }

  downloadPdf(id: string, invoiceNumber: string, event: Event): void {
    event.stopPropagation();
    this.financialService
      .downloadPdf(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          const filename = invoiceNumber.replace(/ /g, '-').replace(/\//g, '-') + '.pdf';
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

  issue(id: string, event: Event): void {
    event.stopPropagation();
    if (this.issuingInvoiceIds.has(id)) return;
    this.error = '';
    this.issuingInvoiceIds.add(id);
    this.financialService
      .issue(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          const idx = this.invoices.findIndex((i) => i.id === updated.id);
          if (idx !== -1) this.invoices[idx] = updated;
          this.issuingInvoiceIds.delete(id);
          this.changeDetectorRef.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao emitir documento.');
          this.issuingInvoiceIds.delete(id);
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadInvoices();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadInvoices();
    }
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
