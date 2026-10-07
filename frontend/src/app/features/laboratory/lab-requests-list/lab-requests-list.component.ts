import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  LabService,
  LabRequestResponse,
  RequestStatus,
  STATUS_LABELS,
  PRIORITY_LABELS,
} from '../../../core/services/lab.service';

@Component({
  selector: 'app-lab-requests-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lab-requests-list.component.html',
  styleUrls: ['./lab-requests-list.component.scss'],
})
export class LabRequestsListComponent implements OnInit {
  requests: LabRequestResponse[] = [];
  loading = true;
  error = '';
  statusFilter: RequestStatus | '' = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  statusLabels = STATUS_LABELS;
  priorityLabels = PRIORITY_LABELS;

  statuses: { value: RequestStatus | ''; label: string }[] = [
    { value: '', label: 'All statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'COLLECTED', label: 'Collected' },
    { value: 'IN_ANALYSIS', label: 'In Analysis' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  constructor(
    private labService: LabService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.labService
      .findAllRequests(undefined, this.statusFilter || undefined, this.currentPage, this.pageSize)
      .subscribe({
        next: (page) => {
          this.requests = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load lab requests.';
          this.loading = false;
        },
      });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadRequests();
  }

  goToCreate(): void {
    this.router.navigate(['/lab/new']);
  }
  goToDetail(id: string): void {
    this.router.navigate(['/lab', id]);
  }

  advanceStatus(req: LabRequestResponse, event: Event): void {
    event.stopPropagation();
    const obs =
      req.status === 'PENDING'
        ? this.labService.collect(req.id)
        : req.status === 'COLLECTED'
          ? this.labService.startAnalysis(req.id)
          : null;

    if (!obs) return;

    obs.subscribe({
      next: (updated) => {
        const idx = this.requests.findIndex((r) => r.id === updated.id);
        if (idx !== -1) this.requests[idx] = updated;
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Failed to update request.';
      },
    });
  }

  getNextActionLabel(status: RequestStatus): string {
    if (status === 'PENDING') return 'Collect Sample';
    if (status === 'COLLECTED') return 'Start Analysis';
    return '';
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadRequests();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadRequests();
    }
  }
}
