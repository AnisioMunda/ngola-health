import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import {
  PrescriptionService,
  PrescriptionResponse,
  PrescriptionStatsDto,
  PRESCRIPTION_STATUS_LABELS,
  STATUS_COLORS,
  prescriptionErrorMessage,
} from '../../../core/services/prescription.service';

function luandaDate(date: Date): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Africa/Luanda',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

@Component({
  selector: 'app-prescriptions-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prescriptions-list.component.html',
  styleUrls: ['./prescriptions-list.component.scss'],
})
export class PrescriptionsListComponent implements OnInit {
  prescriptions: PrescriptionResponse[] = [];
  stats: PrescriptionStatsDto | null = null;
  statsError = '';
  loading = true;
  error = '';

  dateFrom = luandaDate(new Date(Date.now() - 30 * 86400000));
  dateTo = luandaDate(new Date());

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;

  statusLabels = PRESCRIPTION_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor(
    private prescriptionService: PrescriptionService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.canViewList) {
      this.error = 'O seu perfil não tem permissão para consultar a lista de prescrições.';
      this.loading = false;
      return;
    }
    this.loadStats();
    this.load();
  }

  get canViewList(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return ['ADMIN', 'MANAGER', 'DOCTOR', 'PHARMACIST'].some((role) => roles.includes(role));
  }

  get canCreate(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.includes('ADMIN') || roles.includes('DOCTOR');
  }

  loadStats(): void {
    this.statsError = '';
    this.prescriptionService.getStats().subscribe({
      next: (s) => {
        this.stats = s;
      },
      error: (error: unknown) => {
        this.statsError = prescriptionErrorMessage(
          error,
          'Não foi possível carregar as estatísticas das prescrições.',
        );
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    if (this.dateFrom && this.dateTo && this.dateFrom > this.dateTo) {
      this.prescriptions = [];
      this.totalElements = 0;
      this.totalPages = 0;
      this.error = 'A data inicial não pode ser posterior à data final.';
      this.loading = false;
      return;
    }
    this.prescriptionService.findAll(this.dateFrom, this.dateTo, this.currentPage).subscribe({
      next: (page) => {
        this.prescriptions = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.prescriptions = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.error = prescriptionErrorMessage(error, 'Não foi possível carregar as prescrições.');
        this.loading = false;
      },
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  goToNew(): void {
    if (this.canCreate) this.router.navigate(['/prescriptions/new']);
  }
  goToDetail(id: string): void {
    this.router.navigate(['/prescriptions', id]);
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  daysUntilExpiry(expiryDate: string): number {
    const today = Date.parse(`${luandaDate(new Date())}T00:00:00Z`);
    const expiry = Date.parse(`${expiryDate}T00:00:00Z`);
    return Math.ceil((expiry - today) / 86400000);
  }
}
