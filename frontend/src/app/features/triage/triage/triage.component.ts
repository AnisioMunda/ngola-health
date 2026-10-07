import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { EMPTY, interval, Observable, Subscription } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import {
  TriageService,
  TriageResponse,
  TriageStatsDto,
  TriagePriority,
  PRIORITY_LABELS,
  PRIORITY_COLORS,
  PRIORITY_MAX_WAIT,
  STATUS_LABELS,
} from '../../../core/services/triage.service';
import { PatientService } from '../../../core/services/patient.service';

type TriageFormGroup = FormGroup<{
  patientId: FormControl<string>;
  patientNameTemp: FormControl<string>;
  patientAgeTemp: FormControl<number | null>;
  patientGenderTemp: FormControl<string>;
  priority: FormControl<TriagePriority>;
  chiefComplaint: FormControl<string>;
  bloodPressure: FormControl<string>;
  heartRate: FormControl<number | null>;
  temperature: FormControl<number | null>;
  oxygenSaturation: FormControl<number | null>;
  respiratoryRate: FormControl<number | null>;
  weightKg: FormControl<number | null>;
  painScale: FormControl<number | null>;
  triageNotes: FormControl<string>;
}>;

function todayInLuanda(): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Africa/Luanda',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const part = (type: string) => parts.find((item) => item.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

@Component({
  selector: 'app-triage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './triage.component.html',
  styleUrls: ['./triage.component.scss'],
})
export class TriageComponent implements OnInit, OnDestroy {
  queue: TriageResponse[] = [];
  stats: TriageStatsDto | null = null;
  loading = true;
  historyLoading = false;
  saving = false;
  error = '';
  patientError = '';
  success = '';

  showForm = false;
  activeTab: 'queue' | 'history' = 'queue';
  history: TriageResponse[] = [];
  historyError = '';
  historyDate = todayInLuanda();

  private pollingSubscription?: Subscription;
  private successTimeoutId?: ReturnType<typeof setTimeout>;
  private readonly updatingTriageIds = new Set<string>();

  form!: TriageFormGroup;
  patients: { id: string; fullName: string }[] = [];

  priorityLabels = PRIORITY_LABELS;
  priorityColors = PRIORITY_COLORS;
  priorityMaxWait = PRIORITY_MAX_WAIT;
  statusLabels = STATUS_LABELS;

  priorities: { value: TriagePriority; label: string; color: string; desc: string }[] = [
    { value: 'RED', label: 'Vermelho — Imediato', color: '#dc2626', desc: '0 min' },
    { value: 'ORANGE', label: 'Laranja — Muito Urgente', color: '#ea580c', desc: '10 min' },
    { value: 'YELLOW', label: 'Amarelo — Urgente', color: '#ca8a04', desc: '60 min' },
    { value: 'GREEN', label: 'Verde — Pouco Urgente', color: '#16a34a', desc: '120 min' },
    { value: 'BLUE', label: 'Azul — Não Urgente', color: '#2563eb', desc: '240 min' },
  ];

  constructor(
    private fb: FormBuilder,
    private triageService: TriageService,
    private patientService: PatientService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadPatients();
    this.load();
    this.pollingSubscription = interval(30000)
      .pipe(
        switchMap(() =>
          this.triageService.getActiveQueue().pipe(
            catchError((error: unknown) => {
              this.error = this.errorMessage(error, 'Erro ao actualizar a fila de triagem.');
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe({
        next: (queue) => {
          this.queue = queue;
          this.loadStats();
        },
      });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    if (this.successTimeoutId) clearTimeout(this.successTimeoutId);
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId: this.fb.nonNullable.control(''),
      patientNameTemp: this.fb.nonNullable.control(''),
      patientAgeTemp: this.fb.control<number | null>(null, [
        Validators.min(0),
        Validators.max(150),
      ]),
      patientGenderTemp: this.fb.nonNullable.control(''),
      priority: this.fb.nonNullable.control<TriagePriority>('GREEN', Validators.required),
      chiefComplaint: this.fb.nonNullable.control('', Validators.required),
      bloodPressure: this.fb.nonNullable.control(''),
      heartRate: this.fb.control<number | null>(null, Validators.min(0)),
      temperature: this.fb.control<number | null>(null, Validators.min(0)),
      oxygenSaturation: this.fb.control<number | null>(null, [
        Validators.min(0),
        Validators.max(100),
      ]),
      respiratoryRate: this.fb.control<number | null>(null, Validators.min(0)),
      weightKg: this.fb.control<number | null>(null, Validators.min(0)),
      painScale: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(10)]),
      triageNotes: this.fb.nonNullable.control(''),
    });
  }

  loadPatients(): void {
    this.patientError = '';
    this.patientService.findAll('', 0, 200).subscribe({
      next: (page) => {
        this.patients = page.content.map((patient) => ({
          id: patient.id,
          fullName: patient.fullName,
        }));
      },
      error: (error: unknown) => {
        this.patientError = this.errorMessage(error, 'Erro ao carregar a lista de pacientes.');
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.triageService.getActiveQueue().subscribe({
      next: (queue) => {
        this.queue = queue;
        this.loading = false;
        this.loadStats();
      },
      error: (error: unknown) => {
        this.queue = [];
        this.stats = null;
        this.error = this.errorMessage(error, 'Erro ao carregar fila.');
        this.loading = false;
      },
    });
  }

  loadStats(): void {
    this.triageService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (error: unknown) => {
        this.error = this.errorMessage(error, 'Erro ao carregar as estatísticas de triagem.');
      },
    });
  }

  loadHistory(): void {
    this.historyError = '';
    if (!this.historyDate) {
      this.history = [];
      return;
    }
    this.historyLoading = true;
    this.triageService.getHistory(this.historyDate).subscribe({
      next: (history) => {
        this.history = history;
        this.historyLoading = false;
      },
      error: (error: unknown) => {
        this.history = [];
        this.historyError = this.errorMessage(error, 'Erro ao carregar o histórico de triagem.');
        this.historyLoading = false;
      },
    });
  }

  onTabChange(tab: 'queue' | 'history'): void {
    this.activeTab = tab;
    if (tab === 'history') this.loadHistory();
  }

  onPatientChanged(): void {
    if (this.form.controls.patientId.value) {
      this.form.patchValue({
        patientNameTemp: '',
        patientAgeTemp: null,
        patientGenderTemp: '',
      });
    }
  }

  onPriorityChange(triage: TriageResponse, event: Event): void {
    const selectedPriority = (event.target as HTMLSelectElement).value;
    const priority = this.priorities.find((item) => item.value === selectedPriority);
    if (priority) this.updatePriority(triage, priority.value);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const patientId = value.patientId.trim();
    const patientName = value.patientNameTemp.trim();
    if (!patientId && !patientName) {
      this.error = 'Seleccione um paciente ou indique um nome temporário.';
      return;
    }

    this.saving = true;
    this.error = '';
    this.triageService
      .create({
        patientId: patientId || null,
        patientNameTemp: patientId ? null : patientName || null,
        patientAgeTemp: patientId ? null : value.patientAgeTemp,
        patientGenderTemp: patientId ? null : value.patientGenderTemp || null,
        priority: value.priority,
        chiefComplaint: value.chiefComplaint.trim(),
        bloodPressure: value.bloodPressure.trim() || null,
        heartRate: value.heartRate ?? null,
        temperature: value.temperature ?? null,
        oxygenSaturation: value.oxygenSaturation ?? null,
        respiratoryRate: value.respiratoryRate ?? null,
        weightKg: value.weightKg ?? null,
        painScale: value.painScale ?? null,
        triageNotes: value.triageNotes.trim() || null,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.showForm = false;
          this.form.reset({
            patientId: '',
            patientNameTemp: '',
            patientAgeTemp: null,
            patientGenderTemp: '',
            priority: 'GREEN',
            chiefComplaint: '',
            bloodPressure: '',
            heartRate: null,
            temperature: null,
            oxygenSaturation: null,
            respiratoryRate: null,
            weightKg: null,
            painScale: null,
            triageNotes: '',
          });
          this.flash('Triagem registada com sucesso.');
          this.load();
        },
        error: (error: unknown) => {
          this.error = this.errorMessage(error, 'Erro ao criar triagem.');
          this.saving = false;
        },
      });
  }

  callNext(triage: TriageResponse): void {
    this.runTriageAction(
      triage,
      this.triageService.callNext(triage.id),
      (updated) => {
        this.updateInQueue(updated);
        this.flash(`Paciente chamado: ${triage.patientName}`);
      },
      'Erro ao chamar o paciente.',
    );
  }

  complete(triage: TriageResponse): void {
    this.runTriageAction(
      triage,
      this.triageService.complete(triage.id),
      () => {
        this.load();
        this.flash('Atendimento concluído.');
      },
      'Erro ao concluir o atendimento.',
    );
  }

  markAsLeft(triage: TriageResponse): void {
    if (!confirm(`Marcar ${triage.patientName} como saiu sem ser atendido?`)) return;
    this.runTriageAction(
      triage,
      this.triageService.markAsLeft(triage.id),
      () => {
        this.load();
        this.flash('Registo actualizado.');
      },
      'Erro ao actualizar o registo.',
    );
  }

  updatePriority(triage: TriageResponse, priority: TriagePriority): void {
    if (triage.status !== 'WAITING') return;
    this.runTriageAction(
      triage,
      this.triageService.updatePriority(triage.id, priority),
      (updated) => this.updateInQueue(updated),
      'Erro ao actualizar a prioridade.',
    );
  }

  updateInQueue(updated: TriageResponse): void {
    const index = this.queue.findIndex((item) => item.id === updated.id);
    if (index !== -1) this.queue[index] = updated;
    else this.load();
  }

  isUpdating(id: string): boolean {
    return this.updatingTriageIds.has(id);
  }

  flash(message: string): void {
    this.success = message;
    if (this.successTimeoutId) clearTimeout(this.successTimeoutId);
    this.successTimeoutId = setTimeout(() => {
      this.success = '';
      this.successTimeoutId = undefined;
    }, 3000);
  }

  formatWait(minutes: number): string {
    if (minutes < 60) return `${minutes} min`;
    return `${Math.floor(minutes / 60)}h ${minutes % 60}min`;
  }

  get waitingCount(): number {
    return this.queue.filter((item) => item.status === 'WAITING').length;
  }

  get inProgressCount(): number {
    return this.queue.filter((item) => item.status === 'IN_PROGRESS').length;
  }

  get f() {
    return this.form.controls;
  }

  private runTriageAction<T>(
    triage: TriageResponse,
    request: Observable<T>,
    onSuccess: (result: T) => void,
    fallbackError: string,
  ): void {
    if (this.updatingTriageIds.has(triage.id)) return;
    this.error = '';
    this.updatingTriageIds.add(triage.id);
    request.pipe(finalize(() => this.updatingTriageIds.delete(triage.id))).subscribe({
      next: onSuccess,
      error: (error: unknown) => {
        this.error = this.errorMessage(error, fallbackError);
      },
    });
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
      const body = error.error as Record<string, unknown>;
      if (typeof body['detail'] === 'string' && body['detail']) return body['detail'];
      if (typeof body['message'] === 'string' && body['message']) return body['message'];
    }
    return fallback;
  }
}
