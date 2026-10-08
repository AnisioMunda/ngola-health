import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import {
  CreateLabRequestRequest,
  LabService,
  LabTestResponse,
  Priority,
  labErrorMessage,
} from '../../../core/services/lab.service';
import { PatientService } from '../../../core/services/patient.service';
import { AuthService } from '../../../core/services/auth.service';

type LabRequestFormControls = {
  patientId: FormControl<string>;
  priority: FormControl<Priority>;
  clinicalNotes: FormControl<string>;
};

@Component({
  selector: 'app-lab-request-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-request-form.component.html',
  styleUrls: ['./lab-request-form.component.scss'],
})
export class LabRequestFormComponent implements OnInit {
  form!: FormGroup<LabRequestFormControls>;
  saving = false;
  error = '';
  loadingPatients = true;
  loadingTests = true;
  patientsError = '';
  testsError = '';

  patients: { id: string; fullName: string }[] = [];
  availableTests: LabTestResponse[] = [];
  selectedTestIds: Set<string> = new Set();

  priorities: { value: Priority; label: string }[] = [
    { value: 'NORMAL', label: 'Normal' },
    { value: 'URGENT', label: 'Urgent' },
    { value: 'STAT', label: 'STAT (imediata)' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private labService: LabService,
    private patientService: PatientService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.canCreate) {
      void this.router.navigate(['/lab']);
      return;
    }

    this.form = this.fb.group({
      patientId: this.fb.nonNullable.control('', Validators.required),
      priority: this.fb.nonNullable.control<Priority>('NORMAL', Validators.required),
      clinicalNotes: this.fb.nonNullable.control(''),
    });

    this.loadPatients();
    this.loadTests();
  }

  get canCreate(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return ['ADMIN', 'DOCTOR', 'NURSE'].some((role) => roles.includes(role));
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.patientsError = '';
    this.patientService.findAll('', 0, 100).subscribe({
      next: (page) => {
        this.patients = page.content.map((p) => ({ id: p.id, fullName: p.fullName }));
        this.loadingPatients = false;
      },
      error: (error: unknown) => {
        this.patientsError = labErrorMessage(error, 'Não foi possível carregar os pacientes.');
        this.loadingPatients = false;
      },
    });
  }

  loadTests(): void {
    this.loadingTests = true;
    this.testsError = '';
    this.labService.findAllTests().subscribe({
      next: (tests) => {
        this.availableTests = tests.filter((test) => test.active);
        this.loadingTests = false;
      },
      error: (error: unknown) => {
        this.testsError = labErrorMessage(error, 'Não foi possível carregar o catálogo de exames.');
        this.loadingTests = false;
      },
    });
  }

  toggleTest(testId: string): void {
    if (this.selectedTestIds.has(testId)) {
      this.selectedTestIds.delete(testId);
    } else {
      this.selectedTestIds.add(testId);
    }
    this.error = '';
  }

  isTestSelected(testId: string): boolean {
    return this.selectedTestIds.has(testId);
  }

  onSubmit(): void {
    if (
      this.loadingPatients ||
      this.loadingTests ||
      this.patientsError ||
      this.testsError ||
      this.saving
    ) {
      return;
    }
    if (this.form.invalid || this.selectedTestIds.size === 0) {
      this.form.markAllAsTouched();
      if (this.selectedTestIds.size === 0) {
        this.error = 'Seleccione pelo menos um exame.';
      }
      return;
    }

    this.saving = true;
    this.error = '';

    const request: CreateLabRequestRequest = {
      patientId: this.form.controls.patientId.value,
      priority: this.form.controls.priority.value,
      clinicalNotes: this.form.controls.clinicalNotes.value.trim() || null,
      labTestIds: [...this.selectedTestIds],
    };
    this.labService.createRequest(request).subscribe({
      next: () => {
        void this.router.navigate(['/lab']);
      },
      error: (error: unknown) => {
        this.saving = false;
        this.error = labErrorMessage(error, 'Não foi possível criar o pedido de laboratório.');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/lab']);
  }

  get f(): LabRequestFormControls {
    return this.form.controls;
  }
}
