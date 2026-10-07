import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  SchedulingService, AppointmentResponse,
  STATUS_LABELS, TYPE_LABELS
} from '../../../core/services/scheduling.service';

@Component({
  selector: 'app-appointment-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss']
})
export class AppointmentDetailComponent implements OnInit {

  appointment: AppointmentResponse | null = null;
  loading  = true;
  error    = '';
  success  = '';

  cancelReason  = '';
  showCancelForm = false;

  statusLabels = STATUS_LABELS;
  typeLabels   = TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private schedulingService: SchedulingService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading = true;
    this.schedulingService.findById(id).subscribe({
      next: (a) => { this.appointment = a; this.loading = false; },
      error: () => { this.error = 'Erro ao carregar agendamento.'; this.loading = false; }
    });
  }

  confirm(): void {
    if (!this.appointment) return;
    this.schedulingService.confirm(this.appointment.id).subscribe({
      next: (a) => { this.appointment = a; this.flash('Consulta confirmada.'); },
      error: (err) => { this.error = err.error?.message ?? 'Erro ao confirmar.'; }
    });
  }

  complete(): void {
    if (!this.appointment) return;
    this.schedulingService.complete(this.appointment.id).subscribe({
      next: (a) => { this.appointment = a; this.flash('Consulta marcada como realizada.'); },
      error: (err) => { this.error = err.error?.message ?? 'Erro ao completar.'; }
    });
  }

  cancel(): void {
    if (!this.appointment || !this.cancelReason) return;
    this.schedulingService.cancel(this.appointment.id, this.cancelReason).subscribe({
      next: (a) => {
        this.appointment = a;
        this.showCancelForm = false;
        this.cancelReason  = '';
        this.flash('Consulta cancelada.');
      },
      error: (err) => { this.error = err.error?.message ?? 'Erro ao cancelar.'; }
    });
  }

  noShow(): void {
    if (!this.appointment) return;
    this.schedulingService.noShow(this.appointment.id).subscribe({
      next: (a) => { this.appointment = a; this.flash('Falta registada.'); },
      error: (err) => { this.error = err.error?.message ?? 'Erro ao registar falta.'; }
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  goBack(): void { this.router.navigate(['/scheduling']); }
  goToPatient(): void {
    if (this.appointment)
      this.router.navigate(['/patients', this.appointment.patientId, 'edit']);
  }

  canConfirm():  boolean { return this.appointment?.status === 'SCHEDULED'; }
  canComplete(): boolean {
    return this.appointment?.status === 'SCHEDULED'
        || this.appointment?.status === 'CONFIRMED';
  }
  canCancel():   boolean {
    return this.appointment?.status === 'SCHEDULED'
        || this.appointment?.status === 'CONFIRMED';
  }
  canNoShow():   boolean { return this.canCancel(); }
}