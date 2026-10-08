import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import {
  LabService,
  LabRequestResponse,
  RequestStatus,
  STATUS_LABELS,
  PRIORITY_LABELS,
  labErrorMessage,
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
  advancingId: string | null = null;

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  statusLabels = STATUS_LABELS;
  priorityLabels = PRIORITY_LABELS;

  statuses: { value: RequestStatus | ''; label: string }[] = [
    { value: '', label: 'Todos os estados' },
    { value: 'PENDING', label: 'Pendente' },
    { value: 'COLLECTED', label: 'Amostra recolhida' },
    { value: 'IN_ANALYSIS', label: 'Em análise' },
    { value: 'COMPLETED', label: 'Concluído' },
    { value: 'CANCELLED', label: 'Cancelado' },
  ];

  constructor(
    private labService: LabService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.canViewList) {
      this.error = 'O seu perfil não tem permissão para consultar os pedidos de laboratório.';
      this.loading = false;
      return;
    }
    this.loadRequests();
  }

  get canViewList(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return ['ADMIN', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN', 'MANAGER'].some((role) =>
      roles.includes(role),
    );
  }

  get canCreate(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return ['ADMIN', 'DOCTOR', 'NURSE'].some((role) => roles.includes(role));
  }

  canAdvance(status: RequestStatus): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    if (status === 'PENDING') {
      return ['ADMIN', 'NURSE', 'LAB_TECHNICIAN'].some((role) => roles.includes(role));
    }
    if (status === 'COLLECTED') {
      return roles.includes('ADMIN') || roles.includes('LAB_TECHNICIAN');
    }
    return false;
  }

  loadRequests(): void {
    this.loading = true;
    this.error = '';
    this.labService
      .findAllRequests(undefined, this.statusFilter || undefined, this.currentPage, this.pageSize)
      .subscribe({
        next: (page) => {
          this.requests = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: (error: unknown) => {
          this.requests = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.error = labErrorMessage(
            error,
            'Não foi possível carregar os pedidos de laboratório.',
          );
          this.loading = false;
        },
      });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadRequests();
  }

  goToCreate(): void {
    if (this.canCreate) void this.router.navigate(['/lab/new']);
  }
  goToDetail(id: string): void {
    void this.router.navigate(['/lab', id]);
  }

  advanceStatus(req: LabRequestResponse, event: Event): void {
    event.stopPropagation();
    if (!this.canAdvance(req.status) || this.advancingId) return;
    const obs =
      req.status === 'PENDING'
        ? this.labService.collect(req.id)
        : req.status === 'COLLECTED'
          ? this.labService.startAnalysis(req.id)
          : null;

    if (!obs) return;

    this.error = '';
    this.advancingId = req.id;
    obs.subscribe({
      next: (updated) => {
        const idx = this.requests.findIndex((r) => r.id === updated.id);
        if (idx !== -1) this.requests[idx] = updated;
        this.advancingId = null;
      },
      error: (error: unknown) => {
        this.error = labErrorMessage(error, 'Não foi possível actualizar o pedido.');
        this.advancingId = null;
      },
    });
  }

  getNextActionLabel(status: RequestStatus): string {
    if (status === 'PENDING') return 'Recolher amostra';
    if (status === 'COLLECTED') return 'Iniciar análise';
    return '';
  }

  patientInitial(name: string): string {
    return name.trim().charAt(0).toLocaleUpperCase('pt-AO') || '?';
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
