// ============================================================
// portal-dashboard.component.ts
// ============================================================
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  PortalService,
  PortalDashboardDto,
  PortalEpisodeDto,
  PortalPrescriptionDto,
  PortalInvoiceDto,
} from '../../../core/services/portal.service';

@Component({
  selector: 'app-portal-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-dashboard.component.html',
  styleUrls: ['./portal-dashboard.component.scss'],
})
export class PortalDashboardComponent implements OnInit {
  dashboard: PortalDashboardDto | null = null;
  prescriptions: PortalPrescriptionDto[] = [];
  invoices: PortalInvoiceDto[] = [];
  episodes: PortalEpisodeDto[] = [];

  loading = true;
  error = '';

  activeTab: 'home' | 'episodes' | 'prescriptions' | 'invoices' = 'home';

  user = this.portalService.getCurrentUser();

  constructor(
    private portalService: PortalService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.portalService.getDashboard().subscribe({
      next: (d) => {
        this.dashboard = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar dados.';
        this.loading = false;
      },
    });
  }

  onTabChange(tab: 'home' | 'episodes' | 'prescriptions' | 'invoices'): void {
    this.activeTab = tab;
    if (tab === 'prescriptions' && !this.prescriptions.length) {
      this.portalService.getPrescriptions().subscribe((p) => (this.prescriptions = p));
    }
    if (tab === 'invoices' && !this.invoices.length) {
      this.portalService.getInvoices().subscribe((i) => (this.invoices = i));
    }
    if (tab === 'episodes' && !this.episodes.length) {
      this.portalService.getEpisodes().subscribe((e) => (this.episodes = e));
    }
  }

  logout(): void {
    this.portalService.logout();
  }

  getInitials(): string {
    if (!this.user?.patientName) return 'P';
    const parts = this.user.patientName.split(' ').filter((p) => p.length > 0);
    return parts.length >= 2
      ? (parts[0][0] + parts[1][0]).toUpperCase()
      : parts[0][0].toUpperCase();
  }

  statusColor(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: '#3b82f6',
      DISPENSED: '#16a34a',
      CANCELLED: '#dc2626',
      EXPIRED: '#9ca3af',
      PARTIALLY_DISPENSED: '#f59e0b',
      COMPLETED: '#16a34a',
      SCHEDULED: '#3b82f6',
      IN_PROGRESS: '#f59e0b',
      PAID: '#16a34a',
      PENDING: '#f59e0b',
      OVERDUE: '#dc2626',
    };
    return map[status] ?? '#9ca3af';
  }

  invoiceStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PAID: 'Pago',
      PENDING: 'Pendente',
      OVERDUE: 'Em atraso',
      CANCELLED: 'Cancelado',
      ISSUED: 'Emitido',
    };
    return map[status] ?? status;
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('pt-AO', {
      style: 'currency',
      currency: 'AOA',
      minimumFractionDigits: 2,
    }).format(val);
  }

  getPatientFirstName(): string {
    const name = this.dashboard?.patientName;

    if (!name) {
      return '';
    }

    return name.trim().split(/\s+/)[0];
  }
}
