import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import {
  DOSAGE_FORM_LABELS,
  DispenseRequest,
  MedicationResponse,
  PharmacyService,
  ReceiveStockRequest,
  StockBatchResponse,
  pharmacyErrorMessage,
} from '../../../core/services/pharmacy.service';

type ReceiveFormGroup = FormGroup<{
  batchNumber: FormControl<string>;
  expiryDate: FormControl<string>;
  quantity: FormControl<number | null>;
  unitCost: FormControl<number | null>;
  supplier: FormControl<string>;
}>;

type DispenseFormGroup = FormGroup<{
  quantity: FormControl<number | null>;
  reason: FormControl<string>;
}>;

function todayInLuanda(): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Africa/Luanda',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

@Component({
  selector: 'app-medication-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './medication-detail.component.html',
  styleUrls: ['./medication-detail.component.scss'],
})
export class MedicationDetailComponent implements OnInit, OnDestroy {
  medication: MedicationResponse | null = null;
  batches: StockBatchResponse[] = [];
  loading = true;
  batchesLoading = true;
  error = '';
  successMsg = '';
  activeTab: 'batches' | 'receive' | 'dispense' = 'batches';
  readonly formLabels = DOSAGE_FORM_LABELS;
  readonly minimumExpiryDate = todayInLuanda();
  saving = false;
  receiveForm: ReceiveFormGroup;
  dispenseForm: DispenseFormGroup;
  medicationId = '';

  private successTimeoutId?: ReturnType<typeof setTimeout>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private pharmacyService: PharmacyService,
    private authService: AuthService,
  ) {
    this.receiveForm = this.fb.group({
      batchNumber: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(50)]),
      expiryDate: this.fb.nonNullable.control('', Validators.required),
      quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
      unitCost: this.fb.control<number | null>(null, Validators.min(0)),
      supplier: this.fb.nonNullable.control('', Validators.maxLength(200)),
    });
    this.dispenseForm = this.fb.group({
      quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
      reason: this.fb.nonNullable.control('', Validators.maxLength(500)),
    });
  }

  ngOnInit(): void {
    this.medicationId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.medicationId || !this.canViewStockDetails) {
      void this.router.navigate(['/pharmacy']);
      return;
    }
    this.loadMedication();
    this.loadBatches();
  }

  ngOnDestroy(): void {
    if (this.successTimeoutId) clearTimeout(this.successTimeoutId);
  }

  get canViewStockDetails(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => ['ADMIN', 'MANAGER', 'PHARMACIST'].includes(role));
  }

  get canManageStock(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => role === 'ADMIN' || role === 'PHARMACIST');
  }

  loadMedication(): void {
    this.loading = true;
    this.error = '';
    this.pharmacyService.findMedication(this.medicationId).subscribe({
      next: (medication) => {
        this.medication = medication;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.error = pharmacyErrorMessage(error, 'Não foi possível carregar o medicamento.');
        this.loading = false;
      },
    });
  }

  loadBatches(): void {
    this.batchesLoading = true;
    this.pharmacyService.findBatches(this.medicationId).subscribe({
      next: (batches) => {
        this.batches = batches;
        this.batchesLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.error = pharmacyErrorMessage(error, 'Não foi possível carregar os lotes de stock.');
        this.batchesLoading = false;
      },
    });
  }

  receiveStock(): void {
    if (!this.canManageStock || !this.medication || this.receiveForm.invalid) {
      this.receiveForm.markAllAsTouched();
      return;
    }

    const value = this.receiveForm.getRawValue();
    const request: ReceiveStockRequest = {
      medicationId: this.medication.id,
      batchNumber: value.batchNumber.trim(),
      expiryDate: value.expiryDate,
      quantity: value.quantity ?? 0,
      ...(value.unitCost !== null ? { unitCost: value.unitCost } : {}),
      ...(value.supplier.trim() ? { supplier: value.supplier.trim() } : {}),
    };

    this.saving = true;
    this.error = '';
    this.pharmacyService
      .receiveStock(request)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (batch) => {
          this.resetReceiveForm();
          this.flashSuccess(`${batch.quantityReceived} ${this.medication?.unit} recebidos.`);
          this.activeTab = 'batches';
          this.loadMedication();
          this.loadBatches();
        },
        error: (error: HttpErrorResponse) => {
          this.error = pharmacyErrorMessage(error, 'Não foi possível registar a entrada de stock.');
        },
      });
  }

  dispense(): void {
    if (!this.canManageStock || !this.medication || this.dispenseForm.invalid) {
      this.dispenseForm.markAllAsTouched();
      return;
    }

    const value = this.dispenseForm.getRawValue();
    const request: DispenseRequest = {
      medicationId: this.medication.id,
      quantity: value.quantity ?? 0,
      ...(value.reason.trim() ? { reason: value.reason.trim() } : {}),
    };

    this.saving = true;
    this.error = '';
    this.pharmacyService
      .dispense(request)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (result) => {
          this.resetDispenseForm();
          this.flashSuccess(
            `${result.quantityDispensed} ${this.medication?.unit} dispensados. Stock restante: ${result.remainingStock}.`,
          );
          this.activeTab = 'batches';
          this.loadMedication();
          this.loadBatches();
        },
        error: (error: HttpErrorResponse) => {
          this.error = pharmacyErrorMessage(error, 'Não foi possível dispensar o medicamento.');
        },
      });
  }

  isLowStock(): boolean {
    return (
      !!this.medication &&
      this.medication.minStockLevel !== null &&
      this.medication.totalAvailable <= this.medication.minStockLevel
    );
  }

  goBack(): void {
    void this.router.navigate(['/pharmacy']);
  }

  get f() {
    return { receiveForm: this.receiveForm.controls, dispenseForm: this.dispenseForm.controls };
  }

  private resetReceiveForm(): void {
    this.receiveForm.reset({
      batchNumber: '',
      expiryDate: '',
      quantity: null,
      unitCost: null,
      supplier: '',
    });
  }

  private resetDispenseForm(): void {
    this.dispenseForm.reset({ quantity: null, reason: '' });
  }

  private flashSuccess(message: string): void {
    this.successMsg = message;
    if (this.successTimeoutId) clearTimeout(this.successTimeoutId);
    this.successTimeoutId = setTimeout(() => {
      this.successMsg = '';
      this.successTimeoutId = undefined;
    }, 4000);
  }
}
