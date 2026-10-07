import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  FinancialService, InvoiceResponse, InvoiceStatus,
  DOCUMENT_TYPE_LABELS, STATUS_LABELS, PAYMENT_METHOD_LABELS
} from '../../../core/services/financial.service';

@Component({
  selector: 'app-invoices-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './invoices-list.component.html',
  styleUrls: ['./invoices-list.component.scss']
})
export class InvoicesListComponent implements OnInit {

  invoices: InvoiceResponse[] = [];
  loading = true;
  error = '';
  statusFilter: InvoiceStatus | '' = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  docLabels    = DOCUMENT_TYPE_LABELS;
  statusLabels = STATUS_LABELS;
  pmLabels     = PAYMENT_METHOD_LABELS;

  statuses: { value: InvoiceStatus | ''; label: string }[] = [
    { value: '',                 label: 'Todos os estados' },
    { value: 'RASCUNHO',         label: 'Rascunho' },
    { value: 'EMITIDO',          label: 'Emitido' },
    { value: 'PAGO_PARCIALMENTE',label: 'Pago Parcialmente' },
    { value: 'PAGO',             label: 'Pago' },
    { value: 'ANULADO',          label: 'Anulado' },
    { value: 'EM_ATRASO',        label: 'Em Atraso' }
  ];

  constructor(
    private financialService: FinancialService,
    private router: Router
  ) {}

  ngOnInit(): void { this.loadInvoices(); }

  loadInvoices(): void {
    this.loading = true;
    this.financialService.findAllInvoices(
      undefined, this.statusFilter || undefined,
      this.currentPage, this.pageSize
    ).subscribe({
      next: (page) => {
        this.invoices = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Erro ao carregar documentos.'; this.loading = false; }
    });
  }

  onFilterChange(): void { this.currentPage = 0; this.loadInvoices(); }

  goToCreate(): void { this.router.navigate(['/financial/new']); }
  goToDetail(id: string): void { this.router.navigate(['/financial', id]); }

  downloadPdf(id: string, invoiceNumber: string, event: Event): void {
    event.stopPropagation();
    this.financialService.downloadPdf(id).subscribe({
      next: (blob) => {
        const filename = invoiceNumber.replace(/ /g, '-').replace(/\//g, '-') + '.pdf';
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = filename; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.error = 'Erro ao gerar PDF.'; }
    });
  }

  issue(id: string, event: Event): void {
    event.stopPropagation();
    this.financialService.issue(id).subscribe({
      next: (updated) => {
        const idx = this.invoices.findIndex(i => i.id === updated.id);
        if (idx !== -1) this.invoices[idx] = updated;
      },
      error: (err) => { this.error = err.error?.message ?? 'Erro ao emitir documento.'; }
    });
  }

  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.loadInvoices(); } }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.loadInvoices(); } }
}