import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
  TriageService, TriageResponse, TriageStatsDto, TriagePriority,
  PRIORITY_LABELS, PRIORITY_COLORS, PRIORITY_MAX_WAIT, STATUS_LABELS
} from '../../../core/services/triage.service';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-triage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './triage.component.html',
  styleUrls: ['./triage.component.scss']
})
export class TriageComponent implements OnInit, OnDestroy {

  queue:  TriageResponse[]  = [];
  stats:  TriageStatsDto | null = null;
  loading  = true;
  saving   = false;
  error    = '';
  success  = '';

  showForm    = false;
  activeTab: 'queue' | 'history' = 'queue';
  history:    TriageResponse[] = [];
  historyDate = new Date().toISOString().split('T')[0];

  // Polling a cada 30 segundos
  private pollingSubscription?: Subscription;

  form!: FormGroup;
  patients: { id: string; fullName: string }[] = [];

  priorityLabels  = PRIORITY_LABELS;
  priorityColors  = PRIORITY_COLORS;
  priorityMaxWait = PRIORITY_MAX_WAIT;
  statusLabels    = STATUS_LABELS;

  priorities: { value: TriagePriority; label: string; color: string; desc: string }[] = [
    { value: 'RED',    label: 'Vermelho — Imediato',       color: '#dc2626', desc: '0 min' },
    { value: 'ORANGE', label: 'Laranja — Muito Urgente',   color: '#ea580c', desc: '10 min' },
    { value: 'YELLOW', label: 'Amarelo — Urgente',         color: '#ca8a04', desc: '60 min' },
    { value: 'GREEN',  label: 'Verde — Pouco Urgente',     color: '#16a34a', desc: '120 min' },
    { value: 'BLUE',   label: 'Azul — Não Urgente',        color: '#2563eb', desc: '240 min' }
  ];

  constructor(
    private fb: FormBuilder,
    private triageService: TriageService,
    private patientService: PatientService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadPatients();
    this.load();
    // Polling automático a cada 30 segundos
    this.pollingSubscription = interval(30000).pipe(
      switchMap(() => this.triageService.getActiveQueue())
    ).subscribe({
      next: (q) => { this.queue = q; this.loadStats(); }
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId:        [''],
      patientNameTemp:  [''],
      patientAgeTemp:   [''],
      patientGenderTemp:[''],
      priority:         ['GREEN', Validators.required],
      chiefComplaint:   ['', Validators.required],
      bloodPressure:    [''],
      heartRate:        [''],
      temperature:      [''],
      oxygenSaturation: [''],
      respiratoryRate:  [''],
      weightKg:         [''],
      painScale:        [''],
      triageNotes:      ['']
    });
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => {
        this.patients = p.content.map(x => ({ id: x.id, fullName: x.fullName }));
      }
    });
  }

  load(): void {
    this.loading = true;
    this.triageService.getActiveQueue().subscribe({
      next: (q) => { this.queue = q; this.loading = false; this.loadStats(); },
      error: () => { this.error = 'Erro ao carregar fila.'; this.loading = false; }
    });
  }

  loadStats(): void {
    this.triageService.getStats().subscribe({
      next: (s) => { this.stats = s; }
    });
  }

  loadHistory(): void {
    this.triageService.getHistory(this.historyDate).subscribe({
      next: (h) => { this.history = h; }
    });
  }

  onTabChange(tab: 'queue' | 'history'): void {
    this.activeTab = tab;
    if (tab === 'history') this.loadHistory();
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (!v.patientId && !v.patientNameTemp) {
      this.error = 'Seleccione um paciente ou indique um nome temporário.';
      return;
    }
    this.saving = true;
    this.error  = '';

    this.triageService.create({
      patientId:         v.patientId        || null,
      patientNameTemp:   v.patientNameTemp   || null,
      patientAgeTemp:    v.patientAgeTemp    || null,
      patientGenderTemp: v.patientGenderTemp || null,
      priority:          v.priority,
      chiefComplaint:    v.chiefComplaint,
      bloodPressure:     v.bloodPressure     || null,
      heartRate:         v.heartRate         || null,
      temperature:       v.temperature       || null,
      oxygenSaturation:  v.oxygenSaturation  || null,
      respiratoryRate:   v.respiratoryRate   || null,
      weightKg:          v.weightKg          || null,
      painScale:         v.painScale         || null,
      triageNotes:       v.triageNotes       || null
    }).subscribe({
      next: () => {
        this.saving   = false;
        this.showForm = false;
        this.form.reset({ priority: 'GREEN' });
        this.flash('Triagem registada com sucesso.');
        this.load();
      },
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar triagem.';
        this.saving = false;
      }
    });
  }

  callNext(t: TriageResponse): void {
    this.triageService.callNext(t.id).subscribe({
      next: (updated) => {
        this.updateInQueue(updated);
        this.flash('Paciente chamado: ' + t.patientName);
      },
      error: (err) => { this.error = err.error?.message ?? 'Erro.'; }
    });
  }

  complete(t: TriageResponse): void {
    this.triageService.complete(t.id).subscribe({
      next: () => { this.load(); this.flash('Atendimento concluído.'); },
      error: (err) => { this.error = err.error?.message ?? 'Erro.'; }
    });
  }

  markAsLeft(t: TriageResponse): void {
    if (!confirm('Marcar ' + t.patientName + ' como saiu sem ser atendido?')) return;
    this.triageService.markAsLeft(t.id).subscribe({
      next: () => { this.load(); this.flash('Registo actualizado.'); },
      error: (err) => { this.error = err.error?.message ?? 'Erro.'; }
    });
  }

  updatePriority(t: TriageResponse, priority: TriagePriority): void {
    this.triageService.updatePriority(t.id, priority).subscribe({
      next: (updated) => { this.updateInQueue(updated); }
    });
  }

  updateInQueue(updated: TriageResponse): void {
    const idx = this.queue.findIndex(q => q.id === updated.id);
    if (idx !== -1) this.queue[idx] = updated;
    else this.load();
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  formatWait(mins: number): string {
    if (mins < 60) return mins + ' min';
    return Math.floor(mins / 60) + 'h ' + (mins % 60) + 'min';
  }

  get waitingCount():    number { return this.queue.filter(q => q.status === 'WAITING').length; }
  get inProgressCount(): number { return this.queue.filter(q => q.status === 'IN_PROGRESS').length; }

  get f() { return this.form.controls; }
}