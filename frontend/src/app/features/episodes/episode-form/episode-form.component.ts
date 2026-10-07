import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EpisodeService, EpisodeType } from '../../../core/services/episode.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';

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

  patients: { id: string; fullName: string }[] = [];
  doctors: { id: string; fullName: string }[] = [];

  episodeTypes: { value: EpisodeType; label: string }[] = [
    { value: 'OUTPATIENT', label: 'Outpatient Consultation' },
    { value: 'EMERGENCY', label: 'Emergency' },
    { value: 'INPATIENT', label: 'Inpatient / Admission' },
    { value: 'OUTPATIENT_SURGERY', label: 'Outpatient Surgery' },
    { value: 'EXAM', label: 'Exam / Diagnostic' },
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
    });
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (page) => {
        this.doctors = page.content
          .filter((u) => u.roles.includes('DOCTOR'))
          .map((u) => ({ id: u.id, fullName: u.fullName }));
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
          reason: e.reason,
          symptoms: e.symptoms,
          diagnosis: e.diagnosis,
          prescription: e.prescription,
          notes: e.notes,
          bloodPressure: e.bloodPressure,
          heartRate: e.heartRate,
          temperature: e.temperature,
          weightKg: e.weightKg,
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load episode.';
        this.loading = false;
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();

    // Limpar campos vazios
    const payload = Object.fromEntries(
      Object.entries(value).filter(([, v]) => v !== '' && v !== null),
    );

    // Formatar scheduledAt para ISO string
    if (payload['scheduledAt']) {
      payload['scheduledAt'] = new Date(payload['scheduledAt'] as string).toISOString();
    }

    const action = this.isEdit
      ? this.episodeService.update(this.episodeId!, payload as any)
      : this.episodeService.create(payload as any);

    action.subscribe({
      next: () => this.router.navigate(['/episodes']),
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'Failed to save episode.';
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/episodes']);
  }

  get f() {
    return this.form.controls;
  }
}
