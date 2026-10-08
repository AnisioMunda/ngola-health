import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  SchedulingService,
  AppointmentResponse,
  AppointmentStatus,
  STATUS_LABELS,
  TYPE_LABELS,
} from '../../../core/services/scheduling.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-appointments-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointments-list.component.html',
  styleUrls: ['./appointments-list.component.scss'],
})
export class AppointmentsListComponent implements OnInit {
  private loadSequence = 0;

  appointments: AppointmentResponse[] = [];
  loading = true;
  error = '';
  doctorsError = '';

  statusFilter: AppointmentStatus | '' = '';
  doctorFilter = '';
  dateFilter = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;

  doctors: { id: string; fullName: string; especiality: string }[] = [];

  statusLabels = STATUS_LABELS;
  typeLabels = TYPE_LABELS;

  statuses: { value: AppointmentStatus | ''; label: string }[] = [
    { value: '', label: 'Todos os estados' },
    { value: 'SCHEDULED', label: 'Agendado' },
    { value: 'CONFIRMED', label: 'Confirmado' },
    { value: 'COMPLETED', label: 'Realizado' },
    { value: 'CANCELLED', label: 'Cancelado' },
    { value: 'NO_SHOW', label: 'Não Compareceu' },
  ];

  constructor(
    public router: Router, // ← public para usar no template
    private schedulingService: SchedulingService,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.loadDoctors();
    this.load();
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (page) => {
        this.doctors = page.content
          .filter((u) => u.roles?.includes('DOCTOR'))
          .map((u) => ({ id: u.id, fullName: u.fullName, especiality: u.especiality ?? '' }));
        this.doctorsError = '';
      },
      error: () => {
        this.doctorsError = 'Erro ao carregar a lista de médicos.';
      },
    });
  }

  load(): void {
    const requestSequence = ++this.loadSequence;
    this.loading = true;
    this.error = '';
    this.schedulingService
      .findAll(
        this.doctorFilter || undefined,
        undefined,
        this.statusFilter || undefined,
        this.dateFilter || undefined,
        this.currentPage,
      )
      .subscribe({
        next: (page) => {
          if (requestSequence !== this.loadSequence) return;
          this.appointments = page.content;
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.loading = false;
        },
        error: () => {
          if (requestSequence !== this.loadSequence) return;
          this.appointments = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.error = 'Erro ao carregar agendamentos.';
          this.loading = false;
        },
      });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  goToCreate(): void {
    this.router.navigate(['/scheduling/new']);
  }
  goToCalendar(): void {
    this.router.navigate(['/scheduling/calendar']);
  }
  goToSchedules(): void {
    this.router.navigate(['/scheduling/schedules']);
  }
  goToDetail(id: string): void {
    this.router.navigate(['/scheduling', id]);
  }

  confirm(id: string, e: Event): void {
    e.stopPropagation();
    this.schedulingService.confirm(id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao confirmar.';
      },
    });
  }

  cancel(id: string, e: Event): void {
    e.stopPropagation();
    const reason = prompt('Motivo do cancelamento:')?.trim();
    if (!reason) return;
    this.schedulingService.cancel(id, reason).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao cancelar.';
      },
    });
  }

  noShow(id: string, e: Event): void {
    e.stopPropagation();
    this.schedulingService.noShow(id).subscribe({
      next: () => this.load(),
      error: (err) => {
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao registar falta.';
      },
    });
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
}
