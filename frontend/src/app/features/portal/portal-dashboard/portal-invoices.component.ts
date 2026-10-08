import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { PortalInvoiceDto } from '../../../core/services/portal.service';
import { formatAoaCurrency } from '../../../shared/utils/aoa-currency';
import { portalInvoiceStatusLabel, portalStatusColor } from './portal-dashboard.utils';

@Component({
  selector: 'app-portal-invoices',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-invoices.component.html',
})
export class PortalInvoicesComponent {
  @Input() invoices: PortalInvoiceDto[] = [];

  statusColor = portalStatusColor;
  invoiceStatusLabel = portalInvoiceStatusLabel;

  formatCurrency(amount: number): string {
    return formatAoaCurrency(amount);
  }
}
