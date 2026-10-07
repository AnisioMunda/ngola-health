import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PharmacyService, MedicationResponse, StockBatchResponse, DOSAGE_FORM_LABELS } from '../../../core/services/pharmacy.service';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-medication-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './medication-detail.component.html',
  styleUrls: ['./medication-detail.component.scss']
})
export class MedicationDetailComponent implements OnInit {
  medication: MedicationResponse | null = null;
  batches: StockBatchResponse[] = [];
  loading = true;
  error = '';
  successMsg = '';
  activeTab: 'batches' | 'receive' | 'dispense' = 'batches';
  formLabels = DOSAGE_FORM_LABELS;
  patients: { id: string; fullName: string }[] = [];
  saving = false;
  receiveForm!: FormGroup;
  dispenseForm!: FormGroup;
  medicationId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private pharmacyService: PharmacyService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.medicationId = this.route.snapshot.paramMap.get('id') ?? '';
    this.buildForms();
    this.loadPatients();
    if (this.medicationId) {
      this.loadMedication();
      this.loadBatches();
    }
  }

  buildForms(): void {
    this.receiveForm = this.fb.group({
      batchNumber: ['', Validators.required],
      expiryDate:  ['', Validators.required],
      quantity:    [null, [Validators.required, Validators.min(1)]],
      unitCost:    [null],
      supplier:    ['']
    });
    this.dispenseForm = this.fb.group({
      quantity:  [null, [Validators.required, Validators.min(1)]],
      patientId: [''],
      reason:    ['']
    });
  }

  loadMedication(): void {
    this.loading = true;
    this.pharmacyService.findAllMedications('', 0, 1000).subscribe({
      next: (page) => {
        this.medication = page.content.find(m => m.id === this.medicationId) ?? null;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadBatches(): void {
    this.pharmacyService.findBatches(this.medicationId).subscribe({
      next: (b) => { this.batches = b; }
    });
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 100).subscribe({
      next: (p) => { this.patients = p.content.map(x => ({ id: x.id, fullName: x.fullName })); }
    });
  }

  receiveStock(): void {
    if (!this.medication || this.receiveForm.invalid) { this.receiveForm.markAllAsTouched(); return; }
    this.saving = true;
    this.pharmacyService.receiveStock({ medicationId: this.medication.id, ...this.receiveForm.value }).subscribe({
      next: (batch) => {
        this.batches.unshift(batch);
        this.medication!.totalAvailable += batch.quantityAvailable;
        this.receiveForm.reset();
        this.successMsg = 'Stock received successfully.';
        this.activeTab = 'batches';
        this.saving = false;
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err) => { this.error = err.error?.message ?? 'Failed.'; this.saving = false; }
    });
  }

  dispense(): void {
    if (!this.medication || this.dispenseForm.invalid) { this.dispenseForm.markAllAsTouched(); return; }
    this.saving = true;
    this.pharmacyService.dispense({ medicationId: this.medication.id, ...this.dispenseForm.value }).subscribe({
      next: (result) => {
        this.medication!.totalAvailable = result.remainingStock;
        this.loadBatches();
        this.dispenseForm.reset();
        this.successMsg = `Dispensed ${result.quantityDispensed} ${this.medication!.unit}. Remaining: ${result.remainingStock}`;
        this.activeTab = 'batches';
        this.saving = false;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => { this.error = err.error?.message ?? 'Failed.'; this.saving = false; }
    });
  }

  isLowStock(): boolean {
    return !!this.medication && this.medication.totalAvailable <= this.medication.minStockLevel;
  }

  goBack(): void { this.router.navigate(['/pharmacy']); }
  get f() { return { receiveForm: this.receiveForm.controls, dispenseForm: this.dispenseForm.controls }; }
}