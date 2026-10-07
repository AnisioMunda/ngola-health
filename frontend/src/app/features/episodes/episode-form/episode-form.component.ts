import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CreateEpisodeRequest,
  EpisodeService,
  EpisodeType,
  UpdateEpisodeRequest,
} from '../../../core/services/episode.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { HttpErrorResponse } from '@angular/common/http';

interface EpisodeFormValue {
  patientId: string;
  doctorId: string;
  episodeType: EpisodeType;
  scheduledAt: string;
  reason: string;
  symptoms: string;
  diagnosis: string;
  prescription: string;
  notes: string;
  bloodPressure: string;
  heartRate: number | null;
  temperature: number | null;
  weightKg: number | null;
}

@Component({
  selector: 'app-episode-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './episode-form.component.html',
  styleUrls: ['./episode-form.component.scss'],
})
export class EpisodeFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  episodeId: string | null = null;
  loading = false;
  saving = false;
  error = '';
  patientLoadError = '';
  doctorLoadError = '';

  patients: { id: string; fullName: string }[] = [];
  doctors: { id: string; fullName: string }[] = [];

  episodeTypes: { value: EpisodeType; label: string }[] = [
    { value: 'OUTPATIENT', label: 'Consulta externa' },
    { value: 'EMERGENCY', label: 'Urgência' },
    { value: 'INPATIENT', label: 'Internamento' },
    { value: 'OUTPATIENT_SURGERY', label: 'Cirurgia ambulatória' },
    { value: 'EXAM', label: 'Exame / diagnóstico' },
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private episodeService: EpisodeService,
    private patientService: PatientService,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.episodeId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.episodeId;
    this.buildForm();
    this.loadPatients();
    this.loadDoctors();
    if (this.isEdit) this.loadEpisode();
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId: ['', Validators.required],
      doctorId: [''],
      episodeType: ['OUTPATIENT', Validators.required],
      scheduledAt: [''],
      reason: [''],
      symptoms: [''],
      diagnosis: [''],
      prescription: [''],
      notes: [''],
      bloodPressure: [''],
      heartRate: [null],
      temperature: [null],
      weightKg: [null],
    });
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 100).subscribe({
      next: (page) => {
        this.patients = page.content.map((p) => ({ id: p.id, fullName: p.fullName }));
      },
      error: () => {
        this.patientLoadError = 'Não foi possível carregar a lista de pacientes.';
      },
    });
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (page) => {
        this.doctors = page.content
          .filter((u) => u.roles.includes('DOCTOR'))
          .map((u) => ({ id: u.id, fullName: u.fullName }));
      },
      error: () => {
        this.doctorLoadError = 'Não foi possível carregar a lista de médicos.';
      },
    });
  }

  loadEpisode(): void {
    this.loading = true;
    this.episodeService.findById(this.episodeId!).subscribe({
      next: (e) => {
        this.form.patchValue({
          patientId: e.patientId,
          doctorId: e.doctorId ?? '',
          episodeType: e.episodeType,
          scheduledAt: e.scheduledAt ? e.scheduledAt.substring(0, 16) : '',
          reason: e.reason ?? '',
          symptoms: e.symptoms ?? '',
          diagnosis: e.diagnosis ?? '',
          prescription: e.prescription ?? '',
          notes: e.notes ?? '',
          bloodPressure: e.bloodPressure ?? '',
          heartRate: e.heartRate,
          temperature: e.temperature,
          weightKg: e.weightKg,
        });
        this.form.get('patientId')?.disable();
        this.form.get('episodeType')?.disable();
        this.form.get('scheduledAt')?.disable();
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os dados do episódio.';
        this.loading = false;
      },
    });
  }

  onSubmit(): void {
    if (this.saving) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue() as EpisodeFormValue;
    const action = this.isEdit
      ? this.episodeService.update(this.episodeId!, this.toUpdateRequest(value))
      : this.episodeService.create(this.toCreateRequest(value));

    action.subscribe({
      next: () => this.router.navigate(['/episodes']),
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.error =
          error.error?.detail ??
          error.error?.message ??
          'Não foi possível guardar o episódio clínico.';
      },
    });
  }

  private toCreateRequest(value: EpisodeFormValue): CreateEpisodeRequest {
    return {
      patientId: value.patientId,
      doctorId: value.doctorId || undefined,
      episodeType: value.episodeType,
      scheduledAt: value.scheduledAt ? new Date(value.scheduledAt).toISOString() : undefined,
      reason: value.reason.trim() || undefined,
      symptoms: value.symptoms.trim() || undefined,
      diagnosis: value.diagnosis.trim() || undefined,
      prescription: value.prescription.trim() || undefined,
      notes: value.notes.trim() || undefined,
      bloodPressure: value.bloodPressure.trim() || undefined,
      heartRate: value.heartRate ?? undefined,
      temperature: value.temperature ?? undefined,
      weightKg: value.weightKg ?? undefined,
    };
  }

  private toUpdateRequest(value: EpisodeFormValue): UpdateEpisodeRequest {
    return {
      doctorId: value.doctorId || undefined,
      reason: value.reason.trim() || undefined,
      symptoms: value.symptoms.trim() || undefined,
      diagnosis: value.diagnosis.trim() || undefined,
      prescription: value.prescription.trim() || undefined,
      notes: value.notes.trim() || undefined,
      bloodPressure: value.bloodPressure.trim() || undefined,
      heartRate: value.heartRate ?? undefined,
      temperature: value.temperature ?? undefined,
      weightKg: value.weightKg ?? undefined,
    };
  }

  goBack(): void {
    this.router.navigate(['/episodes']);
  }

  get f() {
    return this.form.controls;
  }
}
