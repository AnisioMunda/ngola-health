import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  PharmacyService,
  DOSAGE_FORM_LABELS,
  DosageForm,
} from '../../../core/services/pharmacy.service';

@Component({
  selector: 'app-medication-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div style="padding:2rem;max-width:600px;margin:0 auto">
      <header style="display:flex;align-items:center;gap:16px;margin-bottom:1.5rem">
        <button
          (click)="goBack()"
          style="display:flex;align-items:center;gap:6px;padding:7px 14px;background:none;border:1px solid #e5e7eb;border-radius:8px;font-size:0.875rem;color:#6b7280;cursor:pointer"
        >
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none">
            <path
              d="M12 4L6 10l6 6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
          Back
        </button>
        <h1 style="font-size:1.4rem;font-weight:700;color:#1a1f2e;margin:0">Add Medication</h1>
      </header>

      <form
        [formGroup]="form"
        (ngSubmit)="onSubmit()"
        style="background:white;border:1px solid #e5e7eb;border-radius:14px;padding:1.5rem"
      >
        <div style="margin-bottom:1rem">
          <label
            style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
            >Name *</label
          >
          <input
            type="text"
            formControlName="name"
            placeholder="Amoxicillin"
            style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;box-sizing:border-box;outline:none"
          />
        </div>
        <div style="margin-bottom:1rem">
          <label
            style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
            >Generic Name</label
          >
          <input
            type="text"
            formControlName="genericName"
            placeholder="Generic name"
            style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;box-sizing:border-box;outline:none"
          />
        </div>
        <div style="margin-bottom:1rem">
          <label
            style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
            >Dosage Form *</label
          >
          <select
            formControlName="dosageForm"
            style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;background:white;box-sizing:border-box;outline:none"
          >
            <option value="">Select form...</option>
            <option *ngFor="let f of dosageForms" [value]="f.value">{{ f.label }}</option>
          </select>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem;margin-bottom:1rem">
          <div>
            <label
              style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
              >Strength</label
            >
            <input
              type="text"
              formControlName="strength"
              placeholder="500mg"
              style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;box-sizing:border-box;outline:none"
            />
          </div>
          <div>
            <label
              style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
              >Unit *</label
            >
            <input
              type="text"
              formControlName="unit"
              placeholder="tablet, ml..."
              style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;box-sizing:border-box;outline:none"
            />
          </div>
        </div>
        <div style="margin-bottom:1rem">
          <label
            style="display:block;font-size:0.8rem;font-weight:600;color:#374151;margin-bottom:5px"
            >Min Stock Level</label
          >
          <input
            type="number"
            formControlName="minStockLevel"
            placeholder="10"
            style="width:100%;height:42px;padding:0 12px;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;box-sizing:border-box;outline:none"
          />
        </div>
        <div style="margin-bottom:1.5rem">
          <label
            style="display:flex;align-items:center;gap:10px;cursor:pointer;font-size:0.875rem;color:#374151"
          >
            <input
              type="checkbox"
              formControlName="requiresPrescription"
              style="width:16px;height:16px;accent-color:#203a43"
            />
            Requires Prescription (Rx)
          </label>
        </div>

        <div
          style="padding:12px 16px;background:#fef2f2;border:1px solid #fecaca;border-radius:10px;color:#dc2626;font-size:0.875rem;margin-bottom:1rem"
          *ngIf="error"
        >
          {{ error }}
        </div>

        <div style="display:flex;justify-content:flex-end;gap:12px">
          <button
            type="button"
            (click)="goBack()"
            style="padding:10px 20px;background:white;color:#374151;border:1.5px solid #e5e7eb;border-radius:10px;font-size:0.875rem;font-weight:600;cursor:pointer"
          >
            Cancel
          </button>
          <button
            type="submit"
            [disabled]="saving"
            style="padding:10px 24px;background:linear-gradient(135deg,#203a43,#0f2027);color:white;border:none;border-radius:10px;font-size:0.875rem;font-weight:600;cursor:pointer;opacity:1"
            [style.opacity]="saving ? '0.6' : '1'"
          >
            {{ saving ? 'Saving...' : 'Add Medication' }}
          </button>
        </div>
      </form>
    </div>
  `,
})
export class MedicationFormComponent {
  form: FormGroup;
  saving = false;
  error = '';
  dosageForms = Object.entries(DOSAGE_FORM_LABELS).map(([value, label]) => ({
    value: value as DosageForm,
    label,
  }));

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private pharmacyService: PharmacyService,
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      genericName: [''],
      dosageForm: ['', Validators.required],
      strength: [''],
      unit: ['', Validators.required],
      requiresPrescription: [true],
      minStockLevel: [10],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.pharmacyService.createMedication(this.form.value).subscribe({
      next: () => this.router.navigate(['/pharmacy']),
      error: (err) => {
        this.error = err.error?.message ?? 'Failed to add medication.';
        this.saving = false;
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/pharmacy']);
  }
}
