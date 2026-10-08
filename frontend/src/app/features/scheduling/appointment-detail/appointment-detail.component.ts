import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import {
  SchedulingService,
  AppointmentResponse,
  STATUS_LABELS,
  TYPE_LABELS,
} from '../../../core/services/scheduling.service';

@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss'],
})
export class AppointmentDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private loadSequence = 0;

  appointment: AppointmentResponse | null = null;
  loading = true;
  updating = false;
  error = '';
  success = '';

  cancelReason = '';
  showCancelForm = false;

  statusLabels = STATUS_LABELS;
  typeLabels = TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private schedulingService: SchedulingService,
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.load(id);
      } else {
        this.appointment = null;
        this.error = 'Identificador do agendamento inválido.';
        this.loading = false;
        this.updating = false;
      }
    });
  }

  load(id: string): void {
    const requestSequence = ++this.loadSequence;
    this.loading = true;
    this.error = '';
    this.success = '';
    this.updating = false;
    this.showCancelForm = false;
    this.cancelReason = '';
    this.schedulingService.findById(id).subscribe({
      next: (a) => {
        if (requestSequence !== this.loadSequence) return;
        this.appointment = a;
        this.loading = false;
      },
      error: (err) => {
        if (requestSequence !== this.loadSequence) return;
        this.appointment = null;
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao carregar agendamento.';
        this.loading = false;
      },
    });
  }

  confirm(): void {
    this.updateAppointment((id) => this.schedulingService.confirm(id), 'Consulta confirmada.');
  }

  complete(): void {
    this.updateAppointment(
      (id) => this.schedulingService.complete(id),
      'Consulta marcada como realizada.',
    );
  }

  cancel(): void {
    const reason = this.cancelReason.trim();
    if (!reason || reason.length > 300) return;
    this.updateAppointment(
      (id) => this.schedulingService.cancel(id, reason),
      'Consulta cancelada.',
      () => {
        this.showCancelForm = false;
        this.cancelReason = '';
      },
    );
  }

  noShow(): void {
    this.updateAppointment((id) => this.schedulingService.noShow(id), 'Falta registada.');
  }

  private updateAppointment(
    action: (id: string) => Observable<AppointmentResponse>,
    successMessage: string,
    onSuccess?: () => void,
  ): void {
    const appointmentId = this.appointment?.id;
    if (!appointmentId || this.updating) return;

    this.updating = true;
    this.error = '';
    this.success = '';
    action(appointmentId).subscribe({
      next: (appointment) => {
        if (this.appointment?.id !== appointmentId) return;
        this.appointment = appointment;
        this.updating = false;
        onSuccess?.();
        this.flash(successMessage);
      },
      error: (err) => {
        if (this.appointment?.id !== appointmentId) return;
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao actualizar agendamento.';
        this.updating = false;
      },
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3000);
  }

  goBack(): void {
    this.router.navigate(['/scheduling']);
  }
  goToPatient(): void {
    if (this.appointment) this.router.navigate(['/patients', this.appointment.patientId, 'edit']);
  }

  canConfirm(): boolean {
    return this.appointment?.status === 'SCHEDULED';
  }
  canComplete(): boolean {
    return this.appointment?.status === 'SCHEDULED' || this.appointment?.status === 'CONFIRMED';
  }
  canCancel(): boolean {
    return this.appointment?.status === 'SCHEDULED' || this.appointment?.status === 'CONFIRMED';
  }
  canNoShow(): boolean {
    return this.canCancel();
  }
}
