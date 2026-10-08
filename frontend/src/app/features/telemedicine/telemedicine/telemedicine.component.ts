import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  TelemedicineService,
  SessionResponse,
  TelemedicineStatsDto,
  DoctorOption,
  SESSION_STATUS_COLORS,
} from '../../../core/services/telemedicine.service';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-telemedicine',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './telemedicine.component.html',
  styleUrls: ['./telemedicine.component.scss'],
})
export class TelemedicineComponent implements OnInit, OnDestroy {
  sessions: SessionResponse[] = [];
  stats: TelemedicineStatsDto | null = null;
  loading = true;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  showNotes = false;
  selectedSession: SessionResponse | null = null;
  clinicalNotesText = '';

  patients: { id: string; fullName: string }[] = [];
  doctors: DoctorOption[] = [];

  statusColors = SESSION_STATUS_COLORS;

  form = this.fb.group({
    patientId: ['', Validators.required],
    doctorId: ['', Validators.required],
    scheduledAt: ['', Validators.required],
    durationMinutes: [30, [Validators.required, Validators.min(1), Validators.max(1440)]],
  });

  private pollingSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private telemedicineService: TelemedicineService,
    private patientService: PatientService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadPatients();
    this.loadDoctors();
    this.pollingSub = interval(30000)
      .pipe(switchMap(() => this.telemedicineService.getActiveSessions()))
      .subscribe((s) => {
        this.sessions = s;
        this.loadStats();
      });
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.telemedicineService.getActiveSessions().subscribe({
      next: (s) => {
        this.sessions = s;
        this.loading = false;
        this.loadStats();
      },
      error: () => {
        this.error = 'Erro ao carregar sessões.';
        this.loading = false;
      },
    });
  }

  loadStats(): void {
    this.telemedicineService.getStats().subscribe({ next: (s) => (this.stats = s) });
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => (this.patients = p.content.map((x) => ({ id: x.id, fullName: x.fullName }))),
    });
  }

  loadDoctors(): void {
    this.telemedicineService.getTeamsDoctors().subscribe({
      next: (doctors) => (this.doctors = doctors),
      error: () => {
        this.error = 'Não foi possível carregar os médicos configurados para Microsoft Teams.';
      },
    });
  }

  onCreate(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    const v = this.form.getRawValue();
    if (!v.patientId || !v.doctorId || !v.scheduledAt || v.durationMinutes == null) {
      this.error = 'Indique paciente, médico, data, hora e duração da sessão.';
      this.saving = false;
      return;
    }
    this.telemedicineService
      .create({
        patientId: v.patientId,
        doctorId: v.doctorId,
        scheduledAt: new Date(v.scheduledAt).toISOString(),
        durationMinutes: v.durationMinutes,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.showForm = false;
          this.form.reset();
          this.flash('Sessão criada com sucesso.');
          this.load();
        },
        error: (e) => {
          this.error = e.error?.message ?? 'Erro.';
          this.saving = false;
        },
      });
  }

  joinSession(s: SessionResponse): void {
    const roomUrl = s.roomUrl;
    if (!roomUrl) {
      this.error = 'A ligação do fornecedor de videoconferência ainda não está configurada.';
      return;
    }
    this.telemedicineService.joinSession(s.roomToken).subscribe({
      next: (updated) => {
        this.updateInList(updated);
        window.open(updated.roomUrl ?? roomUrl, '_blank', 'noopener,noreferrer');
      },
      error: (e) => {
        this.error = e.error?.message ?? 'Não foi possível iniciar a sessão.';
      },
    });
  }

  openEndForm(s: SessionResponse): void {
    this.selectedSession = s;
    this.clinicalNotesText = s.clinicalNotes ?? '';
    this.showNotes = true;
  }

  endSession(): void {
    if (!this.selectedSession) return;
    this.telemedicineService.endSession(this.selectedSession.id, this.clinicalNotesText).subscribe({
      next: () => {
        this.showNotes = false;
        this.flash('Sessão concluída.');
        this.load();
      },
      error: (e) => {
        this.error = e.error?.message ?? 'Erro.';
      },
    });
  }

  cancel(s: SessionResponse): void {
    if (!confirm('Cancelar sessão de ' + s.patientName + '?')) return;
    this.telemedicineService.cancel(s.id).subscribe({
      next: () => {
        this.load();
        this.flash('Sessão cancelada.');
      },
      error: (e) => {
        this.error = e.error?.message ?? 'Erro.';
      },
    });
  }

  copyRoomUrl(s: SessionResponse): void {
    if (!s.roomUrl) {
      this.error = 'A ligação do fornecedor de videoconferência ainda não está configurada.';
      return;
    }
    if (!navigator.clipboard?.writeText) {
      this.error = 'O browser não permite copiar a ligação.';
      return;
    }
    navigator.clipboard
      .writeText(s.roomUrl)
      .then(() => this.flash('Link copiado para a área de transferência.'))
      .catch(() => {
        this.error = 'Não foi possível copiar a ligação.';
      });
  }

  updateInList(updated: SessionResponse): void {
    const idx = this.sessions.findIndex((s) => s.id === updated.id);
    if (idx !== -1) this.sessions[idx] = updated;
    else this.load();
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3500);
  }

  get f() {
    return this.form.controls;
  }
}
