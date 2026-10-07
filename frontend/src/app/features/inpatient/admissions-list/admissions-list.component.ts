import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  InpatientService,
  AdmissionResponse,
  AdmissionStatus,
  WardResponse,
  ADMISSION_STATUS_LABELS,
  WARD_TYPE_LABELS,
} from '../../../core/services/inpatient.service';

@Component({
  selector: 'app-admissions-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admissions-list.component.html',
  styleUrls: ['./admissions-list.component.scss'],
})
export class AdmissionsListComponent implements OnInit {
  admissions: AdmissionResponse[] = [];
  wards: WardResponse[] = [];
  loading = true;
  error = '';

  statusFilter: AdmissionStatus | '' = 'ACTIVE';
  wardFilter = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;

  statusLabels = ADMISSION_STATUS_LABELS;
  wardTypeLabels = WARD_TYPE_LABELS;

  statuses: { value: AdmissionStatus | ''; label: string }[] = [
    { value: '', label: 'Todos' },
    { value: 'ACTIVE', label: 'Internado' },
    { value: 'DISCHARGED', label: 'Alta' },
    { value: 'TRANSFERRED', label: 'Transferido' },
    { value: 'DECEASED', label: 'Óbito' },
  ];

  constructor(
    private inpatientService: InpatientService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadWards();
    this.load();
  }

  loadWards(): void {
    this.inpatientService.findAllWards().subscribe({
      next: (w) => {
        this.wards = w;
      },
    });
  }

  load(): void {
    this.loading = true;
    this.inpatientService
      .findAll(this.statusFilter || undefined, this.wardFilter || undefined, this.currentPage)
      .subscribe({
        next: (page) => {
          this.admissions = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: () => {
          this.error = 'Erro ao carregar internamentos.';
          this.loading = false;
        },
      });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  goToMap(): void {
    this.router.navigate(['/inpatient']);
  }
  goToNew(): void {
    this.router.navigate(['/inpatient/admissions/new']);
  }
  goToDetail(id: string): void {
    this.router.navigate(['/inpatient/admissions', id]);
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

  isOverdue(a: AdmissionResponse): boolean {
    if (!a.expectedDischargeDate || a.status !== 'ACTIVE') return false;
    return new Date(a.expectedDischargeDate) < new Date();
  }
}
