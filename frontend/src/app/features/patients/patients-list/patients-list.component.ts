import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { PatientService, PatientResponse } from '../../../core/services/patient.service';
import { ReportService } from '../../../core/services/report.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-patients-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patients-list.component.html',
  styleUrls: ['./patients-list.component.scss'],
})
export class PatientsListComponent implements OnInit {
  patients: PatientResponse[] = [];
  loading = true;
  error = '';
  reportError = '';
  downloadingPatientReport = false;
  searchQuery = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  private searchSubject = new Subject<string>();

  constructor(
    private patientService: PatientService,
    private reportService: ReportService,
    private authService: AuthService,
    private router: Router,
    private destroyRef: DestroyRef,
  ) {}

  ngOnInit(): void {
    this.loadPatients();
    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadPatients();
      });
  }

  loadPatients(): void {
    this.loading = true;
    this.error = '';
    this.patientService.findAll(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.patients = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error =
          err.error?.detail ??
          err.error?.message ??
          'Não foi possível carregar a lista de pacientes.';
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.searchSubject.next(this.searchQuery);
  }

  goToCreate(): void {
    this.router.navigate(['/patients/new']);
  }

  goToDetail(id: string): void {
    this.router.navigate(['/patients', id]);
  }

  get canDownloadPatientReport(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => ['ADMIN', 'DOCTOR', 'NURSE', 'MANAGER'].includes(role));
  }

  downloadPatientReport(id: string, event: Event): void {
    event.stopPropagation();
    if (this.downloadingPatientReport) return;

    this.reportError = '';
    this.downloadingPatientReport = true;
    this.reportService
      .downloadPatientReport(id)
      .pipe(finalize(() => (this.downloadingPatientReport = false)))
      .subscribe({
        next: (blob) => this.reportService.openOrDownload(blob, `ficha-paciente-${id}.pdf`),
        error: () => {
          this.reportError = 'Não foi possível gerar a ficha do paciente. Tente novamente.';
        },
      });
  }

  goToEdit(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/patients', id, 'edit']);
  }

  getGenderLabel(gender: string): string {
    return gender === 'MALE' ? 'Masculino' : 'Feminino';
  }

  getBloodTypeBadge(type: string): string {
    return type || '—';
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadPatients();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadPatients();
    }
  }
}
