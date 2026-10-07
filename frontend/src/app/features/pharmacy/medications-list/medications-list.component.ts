import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ReportService } from '../../../core/services/report.service';
import {
  PharmacyService,
  MedicationResponse,
  DOSAGE_FORM_LABELS,
  pharmacyErrorMessage,
} from '../../../core/services/pharmacy.service';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-medications-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medications-list.component.html',
  styleUrls: ['./medications-list.component.scss'],
})
export class MedicationsListComponent implements OnInit, OnDestroy {
  medications: MedicationResponse[] = [];
  loading = true;
  error = '';
  reportError = '';
  downloadingReport = false;
  searchQuery = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  formLabels = DOSAGE_FORM_LABELS;
  private searchSubject = new Subject<string>();

  constructor(
    private pharmacyService: PharmacyService,
    private reportService: ReportService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged()).subscribe(() => {
      this.currentPage = 0;
      this.loadMedications();
    });
    this.loadMedications();
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  loadMedications(): void {
    this.loading = true;
    this.error = '';
    this.pharmacyService
      .findAllMedications(this.searchQuery, this.currentPage, this.pageSize)
      .subscribe({
        next: (page) => {
          this.medications = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: (error: unknown) => {
          this.medications = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.error = pharmacyErrorMessage(error, 'Não foi possível carregar os medicamentos.');
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    this.searchSubject.next(this.searchQuery);
  }

  goToMedication(id: string): void {
    if (!this.canViewStockDetails) return;
    this.router.navigate(['/pharmacy', id]);
  }

  goToAdd(): void {
    if (!this.canManagePharmacy) return;
    this.router.navigate(['/pharmacy/new']);
  }

  goToExpiring(): void {
    if (!this.canViewStockDetails) return;
    this.router.navigate(['/pharmacy/expiring']);
  }

  downloadStockReport(): void {
    if (!this.canViewStockDetails || this.downloadingReport) return;

    this.downloadingReport = true;
    this.reportError = '';
    this.reportService
      .downloadStockReport()
      .pipe(finalize(() => (this.downloadingReport = false)))
      .subscribe({
        next: (blob) => this.reportService.openOrDownload(blob, 'inventario-stock.pdf'),
        error: (error: unknown) => {
          this.reportError = pharmacyErrorMessage(
            error,
            'Não foi possível gerar o inventário de stock em PDF.',
          );
        },
      });
  }

  get canManagePharmacy(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => role === 'ADMIN' || role === 'PHARMACIST');
  }

  get canViewStockDetails(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => ['ADMIN', 'MANAGER', 'PHARMACIST'].includes(role));
  }

  isLowStock(med: MedicationResponse): boolean {
    return med.totalAvailable <= (med.minStockLevel ?? 0);
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadMedications();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadMedications();
    }
  }
}
