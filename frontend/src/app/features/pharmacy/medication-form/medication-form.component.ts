import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import {
  CreateMedicationRequest,
  DOSAGE_FORM_LABELS,
  DosageForm,
  PharmacyService,
  pharmacyErrorMessage,
} from '../../../core/services/pharmacy.service';

type MedicationFormGroup = FormGroup<{
  name: FormControl<string>;
  genericName: FormControl<string>;
  dosageForm: FormControl<DosageForm | ''>;
  strength: FormControl<string>;
  unit: FormControl<string>;
  requiresPrescription: FormControl<boolean>;
  minStockLevel: FormControl<number | null>;
}>;

@Component({
  selector: 'app-medication-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medication-form.component.html',
  styleUrls: ['./medication-form.component.scss'],
})
export class MedicationFormComponent implements OnInit {
  form: MedicationFormGroup;
  saving = false;
  error = '';
  readonly dosageForms = Object.entries(DOSAGE_FORM_LABELS).map(([value, label]) => ({
    value: value as DosageForm,
    label,
  }));

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private pharmacyService: PharmacyService,
    private authService: AuthService,
  ) {
    this.form = this.fb.group({
      name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
      genericName: this.fb.nonNullable.control('', Validators.maxLength(200)),
      dosageForm: this.fb.nonNullable.control<DosageForm | ''>('', Validators.required),
      strength: this.fb.nonNullable.control('', Validators.maxLength(50)),
      unit: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
      requiresPrescription: this.fb.nonNullable.control(true),
      minStockLevel: this.fb.control<number | null>(10, Validators.min(0)),
    });
  }

  ngOnInit(): void {
    if (!this.canManagePharmacy) void this.router.navigate(['/pharmacy']);
  }

  get canManagePharmacy(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => role === 'ADMIN' || role === 'PHARMACIST');
  }

  onSubmit(): void {
    if (!this.canManagePharmacy || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    if (!value.dosageForm) return;

    const request: CreateMedicationRequest = {
      name: value.name.trim(),
      genericName: value.genericName.trim() || null,
      dosageForm: value.dosageForm,
      strength: value.strength.trim() || null,
      unit: value.unit.trim(),
      requiresPrescription: value.requiresPrescription,
      minStockLevel: value.minStockLevel ?? 10,
    };

    this.saving = true;
    this.error = '';
    this.pharmacyService
      .createMedication(request)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => void this.router.navigate(['/pharmacy']),
        error: (error: HttpErrorResponse) => {
          this.error = pharmacyErrorMessage(error, 'Não foi possível adicionar o medicamento.');
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/pharmacy']);
  }

  get f() {
    return this.form.controls;
  }
}
