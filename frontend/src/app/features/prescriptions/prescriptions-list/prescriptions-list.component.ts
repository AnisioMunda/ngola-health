import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  PrescriptionService,
  PrescriptionResponse,
  PrescriptionStatsDto,
  PRESCRIPTION_STATUS_LABELS,
  STATUS_COLORS,
} from '../../../core/services/prescription.service';

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
  loading = true;
  error = '';

  dateFrom = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
  dateTo = new Date().toISOString().split('T')[0];

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;

  statusLabels = PRESCRIPTION_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor(
    private prescriptionService: PrescriptionService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.load();
  }

  loadStats(): void {
    this.prescriptionService.getStats().subscribe({
      next: (s) => {
        this.stats = s;
      },
    });
  }

  load(): void {
    this.loading = true;
    this.prescriptionService.findAll(this.dateFrom, this.dateTo, this.currentPage).subscribe({
      next: (page) => {
        this.prescriptions = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar prescrições.';
        this.loading = false;
      },
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  goToNew(): void {
    this.router.navigate(['/prescriptions/new']);
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
    return Math.ceil((new Date(expiryDate).getTime() - Date.now()) / 86400000);
  }
}
