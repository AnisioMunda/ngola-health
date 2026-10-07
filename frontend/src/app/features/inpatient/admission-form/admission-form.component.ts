import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CreateAdmissionRequest,
  InpatientService,
  WardResponse,
  BedResponse,
} from '../../../core/services/inpatient.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-admission-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admission-form.component.html',
  styleUrls: ['./admission-form.component.scss'],
})
export class AdmissionFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private bedLoadSequence = 0;
  private pendingBedId: string | null = null;

  form!: FormGroup<{
    patientId: FormControl<string>;
    wardId: FormControl<string>;
    bedId: FormControl<string>;
    responsibleDoctorId: FormControl<string>;
    admissionReason: FormControl<string>;
    expectedDischargeDate: FormControl<string>;
  }>;
  saving = false;
  error = '';

  patients: { id: string; fullName: string; phone: string }[] = [];
  doctors: { id: string; fullName: string }[] = [];
  wards: WardResponse[] = [];
  beds: BedResponse[] = [];

  loadingPatients = false;
  loadingDoctors = false;
  loadingWards = false;
  loadingBeds = false;
  bedsError = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private inpatientService: InpatientService,
    private patientService: PatientService,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.pendingBedId = this.route.snapshot.queryParamMap.get('bedId');
    this.loadPatients();
    this.loadDoctors();
    this.loadWards(this.route.snapshot.queryParamMap.get('wardId') ?? undefined);
  }

  buildForm(): void {
    this.form = this.fb.nonNullable.group({
      patientId: ['', Validators.required],
      wardId: ['', Validators.required],
      bedId: ['', Validators.required],
      responsibleDoctorId: ['', Validators.required],
      admissionReason: [
        '',
        [
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(500),
          Validators.pattern(/\S/),
        ],
      ],
      expectedDischargeDate: [''],
    });

    this.form.controls.wardId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((wardId) => this.loadBeds(wardId));
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.patientService
      .findAll('', 0, 200)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.patients = page.content.map((patient) => ({
            id: patient.id,
            fullName: patient.fullName,
            phone: patient.phone ?? '',
          }));
          this.loadingPatients = false;
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar pacientes.');
          this.loadingPatients = false;
        },
      });
  }

  loadDoctors(): void {
    this.loadingDoctors = true;
    this.userService
      .findAll(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.doctors = page.content
            .filter((user) => user.roles?.includes('DOCTOR'))
            .map((user) => ({ id: user.id, fullName: user.fullName }));
          this.loadingDoctors = false;
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar médicos.');
          this.loadingDoctors = false;
        },
      });
  }

  loadWards(preferredWardId?: string): void {
    this.loadingWards = true;
    this.inpatientService
      .findAllWards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (wards) => {
          this.wards = wards;
          this.loadingWards = false;
          if (!preferredWardId) return;
          if (!wards.some((ward) => ward.id === preferredWardId)) {
            this.pendingBedId = null;
            this.error = 'A enfermaria seleccionada já não está disponível.';
            return;
          }
          this.form.controls.wardId.setValue(preferredWardId);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar enfermarias.');
          this.loadingWards = false;
        },
      });
  }

  loadBeds(wardId: string): void {
    const requestSequence = ++this.bedLoadSequence;
    this.bedsError = '';
    this.form.controls.bedId.setValue('');
    this.beds = [];
    if (!wardId) {
      this.loadingBeds = false;
      return;
    }

    this.loadingBeds = true;
    this.inpatientService
      .findBedsByWard(wardId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (beds) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.beds = beds.filter((bed) => bed.status === 'AVAILABLE' || bed.status === 'RESERVED');
          this.loadingBeds = false;
          if (!this.pendingBedId) return;

          if (this.beds.some((bed) => bed.id === this.pendingBedId)) {
            this.form.controls.bedId.setValue(this.pendingBedId);
          } else {
            this.error = 'A cama seleccionada já não está disponível. Escolha outra cama.';
          }
          this.pendingBedId = null;
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.bedsError = this.errorMessage(error, 'Erro ao carregar camas disponíveis.');
          this.error = this.bedsError;
          this.loadingBeds = false;
        },
      });
  }

  selectBed(bedId: string): void {
    this.form.controls.bedId.setValue(bedId);
    this.form.controls.bedId.markAsTouched();
  }

  onSubmit(): void {
    if (this.saving) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';

    const value = this.form.getRawValue();
    const request: CreateAdmissionRequest = {
      patientId: value.patientId,
      bedId: value.bedId,
      responsibleDoctorId: value.responsibleDoctorId,
      admissionReason: value.admissionReason.trim(),
      expectedDischargeDate: value.expectedDischargeDate || null,
    };
    this.inpatientService
      .admit(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (admission) => {
          this.saving = false;
          this.router.navigate(['/inpatient/admissions', admission.id]);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao criar internamento.');
          this.saving = false;
        },
      });
  }

  get minDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  get f() {
    return this.form.controls;
  }

  goBack(): void {
    this.router.navigate(['/inpatient/admissions']);
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
