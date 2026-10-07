import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PatientService,
  ANGOLA_PROVINCES,
  BLOOD_TYPES,
  CheckPatientDuplicatesRequest,
  CreatePatientRequest,
  PatientDuplicateCandidate,
} from '../../../core/services/patient.service';

type PatientFormTab = 'personal' | 'contact' | 'clinical';

function pastDateValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const selectedDate = new Date(`${control.value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Number.isNaN(selectedDate.getTime()) || selectedDate >= today ? { past: true } : null;
}

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.scss'],
})
export class PatientFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  patientId: string | null = null;
  loading = false;
  saving = false;
  checkingDuplicates = false;
  error = '';
  possibleDuplicates: PatientDuplicateCandidate[] = [];

  provinces = ANGOLA_PROVINCES;
  bloodTypes = BLOOD_TYPES;
  activeTab: PatientFormTab = 'personal';
  private pendingRequest: CreatePatientRequest | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
  ) {}

  ngOnInit(): void {
    this.patientId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.patientId;
    this.buildForm();
    if (this.isEdit) this.loadPatient();
  }

  buildForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      birthDate: ['', [Validators.required, pastDateValidator]],
      gender: ['', Validators.required],
      nationalId: [
        '',
        [Validators.maxLength(20), Validators.pattern(/^\s*(?:\d{9}[A-Z]{2}\d{3}|\d{10})?\s*$/i)],
      ],
      healthCardNumber: ['', Validators.maxLength(30)],
      phone: ['', Validators.maxLength(20)],
      email: ['', [Validators.email, Validators.maxLength(200)]],
      address: ['', Validators.maxLength(500)],
      province: ['', Validators.maxLength(100)],
      municipality: ['', Validators.maxLength(100)],
      emergencyContactName: ['', Validators.maxLength(200)],
      emergencyContactPhone: ['', Validators.maxLength(20)],
      emergencyContactRelationship: ['', Validators.maxLength(50)],
      bloodType: [''],
      allergies: [''],
      chronicConditions: [''],
      notes: [''],
    });
  }

  loadPatient(): void {
    this.loading = true;
    this.patientService.findById(this.patientId!).subscribe({
      next: (p) => {
        this.form.patchValue({
          ...p,
          birthDate: p.birthDate?.substring(0, 10),
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os dados do paciente.';
        this.loading = false;
      },
    });
  }

  setTab(tab: PatientFormTab): void {
    this.activeTab = tab;
  }

  onSubmit(): void {
    if (this.saving || this.possibleDuplicates.length > 0) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      const tabWithErrors = (Object.keys(this.tabFields) as PatientFormTab[]).find((tab) =>
        this.tabFields[tab].some((field) => this.form.get(field)?.invalid),
      );
      this.activeTab = tabWithErrors ?? 'personal';
      return;
    }

    this.error = '';
    const request = this.toRequest();

    if (this.isEdit) {
      this.savePatient(request);
      return;
    }

    this.checkForPossibleDuplicates(request);
  }

  confirmRegistrationDespiteDuplicates(): void {
    if (!this.pendingRequest || this.saving) return;
    this.savePatient(this.pendingRequest);
  }

  cancelRegistrationDespiteDuplicates(): void {
    this.pendingRequest = null;
    this.possibleDuplicates = [];
    this.form.enable();
  }

  private checkForPossibleDuplicates(request: CreatePatientRequest): void {
    this.checkingDuplicates = true;
    this.saving = true;
    const duplicateCheck: CheckPatientDuplicatesRequest = {
      fullName: request.fullName,
      birthDate: request.birthDate,
      phone: request.phone,
    };

    this.patientService.findPossibleDuplicates(duplicateCheck).subscribe({
      next: (candidates) => {
        this.checkingDuplicates = false;
        if (candidates.length > 0) {
          this.possibleDuplicates = candidates;
          this.pendingRequest = request;
          this.form.disable();
          this.saving = false;
          return;
        }
        this.savePatient(request);
      },
      error: (err: HttpErrorResponse) => {
        this.checkingDuplicates = false;
        this.saving = false;
        this.error = this.getErrorMessage(
          err,
          'Não foi possível verificar possíveis duplicados. Tente novamente.',
        );
      },
    });
  }

  private savePatient(request: CreatePatientRequest): void {
    this.saving = true;
    this.error = '';
    const action = this.isEdit
      ? this.patientService.update(this.patientId!, request)
      : this.patientService.create(request);

    action.subscribe({
      next: () => this.router.navigate(['/patients']),
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = this.getErrorMessage(err, 'Não foi possível guardar os dados do paciente.');
      },
    });
  }

  private toRequest(): CreatePatientRequest {
    const value = this.form.getRawValue();
    return {
      fullName: value.fullName.trim(),
      birthDate: value.birthDate,
      gender: value.gender,
      nationalId: this.optionalValue(value.nationalId),
      healthCardNumber: this.optionalValue(value.healthCardNumber),
      phone: this.optionalValue(value.phone),
      email: this.optionalValue(value.email),
      address: this.optionalValue(value.address),
      province: this.optionalValue(value.province),
      municipality: this.optionalValue(value.municipality),
      emergencyContactName: this.optionalValue(value.emergencyContactName),
      emergencyContactPhone: this.optionalValue(value.emergencyContactPhone),
      emergencyContactRelationship: this.optionalValue(value.emergencyContactRelationship),
      bloodType: this.optionalValue(value.bloodType),
      allergies: this.optionalValue(value.allergies),
      chronicConditions: this.optionalValue(value.chronicConditions),
      notes: this.optionalValue(value.notes),
    };
  }

  private optionalValue(value: string | null | undefined): string | undefined {
    const normalized = value?.trim();
    return normalized || undefined;
  }

  private getErrorMessage(error: HttpErrorResponse, fallback: string): string {
    const body = error.error as { detail?: unknown; message?: unknown } | null;
    if (typeof body?.detail === 'string') return body.detail;
    if (typeof body?.message === 'string') return body.message;
    return fallback;
  }

  private readonly tabFields: Record<PatientFormTab, string[]> = {
    personal: ['fullName', 'birthDate', 'gender', 'nationalId', 'healthCardNumber'],
    contact: [
      'phone',
      'email',
      'address',
      'province',
      'municipality',
      'emergencyContactName',
      'emergencyContactPhone',
      'emergencyContactRelationship',
    ],
    clinical: ['bloodType'],
  };

  goBack(): void {
    this.router.navigate(['/patients']);
  }

  get f() {
    return this.form.controls;
  }
}
