import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  FormControl,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CreatePrescriptionRequest,
  PrescriptionService,
  ROUTES,
  prescriptionErrorMessage,
} from '../../../core/services/prescription.service';
import { PatientService } from '../../../core/services/patient.service';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { AuthService } from '../../../core/services/auth.service';

type PrescriptionItemControls = {
  medicationId: FormControl<string>;
  quantityPrescribed: FormControl<number | null>;
  dosage: FormControl<string>;
  frequencyHours: FormControl<number | null>;
  durationDays: FormControl<number | null>;
  route: FormControl<string>;
  instructions: FormControl<string>;
};

type PrescriptionControls = {
  patientId: FormControl<string>;
  episodeId: FormControl<string>;
  admissionId: FormControl<string>;
  diagnosis: FormControl<string>;
  notes: FormControl<string>;
  validityDays: FormControl<number | null>;
  items: FormArray<FormGroup<PrescriptionItemControls>>;
};

@Component({
  selector: 'app-prescription-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './prescription-form.component.html',
  styleUrls: ['./prescription-form.component.scss'],
})
export class PrescriptionFormComponent implements OnInit {
  form!: FormGroup<PrescriptionControls>;
  saving = false;
  error = '';
  loadingPatients = true;
  loadingMedications = true;
  patientsError = '';
  medicationsError = '';

  patients: { id: string; fullName: string }[] = [];
  medications: { id: string; name: string; unit: string; stock: number }[] = [];

  routes = ROUTES;

  // Pré-selecção por query params
  prePatientId: string | null = null;
  preEpisodeId: string | null = null;
  preAdmissionId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private changeDetectorRef: ChangeDetectorRef,
    private router: Router,
    private route: ActivatedRoute,
    private prescriptionService: PrescriptionService,
    private patientService: PatientService,
    private pharmacyService: PharmacyService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    if (!roles.includes('ADMIN') && !roles.includes('DOCTOR')) {
      void this.router.navigate(['/prescriptions']);
      return;
    }

    this.prePatientId = this.route.snapshot.queryParamMap.get('patientId');
    this.preEpisodeId = this.route.snapshot.queryParamMap.get('episodeId');
    this.preAdmissionId = this.route.snapshot.queryParamMap.get('admissionId');

    this.buildForm();
    this.loadPatients();
    this.loadMedications();
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId: this.fb.nonNullable.control(this.prePatientId ?? '', Validators.required),
      episodeId: this.fb.nonNullable.control(this.preEpisodeId ?? ''),
      admissionId: this.fb.nonNullable.control(this.preAdmissionId ?? ''),
      diagnosis: this.fb.nonNullable.control('', Validators.maxLength(500)),
      notes: this.fb.nonNullable.control('', Validators.maxLength(2000)),
      validityDays: this.fb.control(30, [
        Validators.required,
        Validators.min(1),
        Validators.max(365),
      ]),
      items: this.fb.array<FormGroup<PrescriptionItemControls>>([]),
    });

    // Adicionar pelo menos um item
    this.addItem();
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.patientsError = '';
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => {
        this.patients = p.content.map((x) => ({ id: x.id, fullName: x.fullName }));
        this.loadingPatients = false;
        this.changeDetectorRef.markForCheck();
      },
      error: (error: unknown) => {
        this.patientsError = prescriptionErrorMessage(
          error,
          'Não foi possível carregar os pacientes.',
        );
        this.loadingPatients = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  loadMedications(): void {
    this.loadingMedications = true;
    this.medicationsError = '';
    this.pharmacyService.findAllMedications('', 0, 200).subscribe({
      next: (p) => {
        this.medications = p.content
          .filter((medication) => medication.active)
          .map((medication) => ({
            id: medication.id,
            name: medication.name,
            unit: medication.unit,
            stock: medication.totalAvailable,
          }));
        this.loadingMedications = false;
        this.changeDetectorRef.markForCheck();
      },
      error: (error: unknown) => {
        this.medicationsError = prescriptionErrorMessage(
          error,
          'Não foi possível carregar os medicamentos.',
        );
        this.loadingMedications = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  get items(): FormArray<FormGroup<PrescriptionItemControls>> {
    return this.form.controls.items;
  }

  addItem(): void {
    this.items.push(
      new FormGroup<PrescriptionItemControls>({
        medicationId: this.fb.nonNullable.control('', Validators.required),
        quantityPrescribed: this.fb.control(1, [Validators.required, Validators.min(1)]),
        dosage: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
        frequencyHours: this.fb.control(8, Validators.min(1)),
        durationDays: this.fb.control(7, Validators.min(1)),
        route: this.fb.nonNullable.control('Oral', Validators.maxLength(50)),
        instructions: this.fb.nonNullable.control('', Validators.maxLength(300)),
      }),
    );
  }

  removeItem(i: number): void {
    if (this.items.length > 1) this.items.removeAt(i);
  }

  getMedication(id: string) {
    return this.medications.find((m) => m.id === id);
  }

  onSubmit(): void {
    if (
      this.loadingPatients ||
      this.loadingMedications ||
      this.patientsError ||
      this.medicationsError
    ) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';

    const value = this.form.getRawValue();
    const request: CreatePrescriptionRequest = {
      patientId: value.patientId,
      episodeId: value.episodeId || null,
      admissionId: value.admissionId || null,
      diagnosis: value.diagnosis.trim() || null,
      notes: value.notes.trim() || null,
      validityDays: value.validityDays ?? 30,
      items: value.items.map((item) => ({
        medicationId: item.medicationId,
        quantityPrescribed: item.quantityPrescribed ?? 0,
        dosage: item.dosage.trim(),
        frequencyHours: item.frequencyHours,
        durationDays: item.durationDays,
        route: item.route || null,
        instructions: item.instructions.trim() || null,
      })),
    };
    this.prescriptionService.create(request).subscribe({
      next: (p) => {
        void this.router.navigate(['/prescriptions', p.id]);
      },
      error: (error: unknown) => {
        this.error = prescriptionErrorMessage(error, 'Não foi possível criar a prescrição.');
        this.saving = false;
        this.changeDetectorRef.markForCheck();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/prescriptions']);
  }
  get f(): PrescriptionControls {
    return this.form.controls;
  }
}
