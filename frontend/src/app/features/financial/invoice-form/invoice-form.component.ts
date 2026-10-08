import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  FormControl,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FinancialService,
  ServicePriceResponse,
  DocumentType,
} from '../../../core/services/financial.service';
import { PatientService } from '../../../core/services/patient.service';

type InvoiceItemControls = {
  servicePriceId: FormControl<string>;
  description: FormControl<string>;
  quantity: FormControl<number>;
  unitPrice: FormControl<number | null>;
  discountPercent: FormControl<number>;
  vatRate: FormControl<number>;
};

type InvoiceFormControls = {
  patientId: FormControl<string>;
  documentType: FormControl<DocumentType>;
  patientNif: FormControl<string>;
  patientFiscalName: FormControl<string>;
  insuranceProvider: FormControl<string>;
  insurancePolicyNumber: FormControl<string>;
  insuranceCoveragePercent: FormControl<number>;
  dueDate: FormControl<string>;
  notes: FormControl<string>;
  items: FormArray<FormGroup<InvoiceItemControls>>;
};

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './invoice-form.component.html',
  styleUrls: ['./invoice-form.component.scss'],
})
export class InvoiceFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup<InvoiceFormControls>;
  saving = false;
  error = '';

  patients: { id: string; fullName: string }[] = [];
  servicePrices: ServicePriceResponse[] = [];
  patientsLoading = true;
  pricesLoading = true;
  patientsError = '';
  pricesError = '';

  docTypes: { value: DocumentType; label: string; desc: string }[] = [
    { value: 'FR', label: 'FR — Factura/Recibo', desc: 'Com pagamento no acto' },
    { value: 'FT', label: 'FT — Factura', desc: 'Sem pagamento imediato' },
    { value: 'NC', label: 'NC — Nota de Crédito', desc: 'Devolução / anulação' },
    { value: 'ND', label: 'ND — Nota de Débito', desc: 'Cobrança adicional' },
  ];

  constructor(
    private fb: FormBuilder,
    private changeDetectorRef: ChangeDetectorRef,
    private router: Router,
    private financialService: FinancialService,
    private patientService: PatientService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadPatients();
    this.loadPrices();
  }

  buildForm(): void {
    this.form = this.fb.nonNullable.group({
      patientId: ['', Validators.required],
      documentType: ['FR' as DocumentType, Validators.required],
      patientNif: [''],
      patientFiscalName: [''],
      insuranceProvider: [''],
      insurancePolicyNumber: [''],
      insuranceCoveragePercent: [0, [Validators.min(0), Validators.max(100)]],
      dueDate: [''],
      notes: [''],
      items: this.fb.array([this.newItem()]),
    });
  }

  get items(): FormArray<FormGroup<InvoiceItemControls>> {
    return this.form.controls.items;
  }

  newItem(): FormGroup<InvoiceItemControls> {
    return this.fb.nonNullable.group({
      servicePriceId: [''],
      description: ['', [Validators.required, Validators.pattern(/\S/)]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
      discountPercent: [0, [Validators.min(0), Validators.max(100)]],
      vatRate: [0, [Validators.min(0), Validators.max(100)]],
    });
  }

  addItem(): void {
    this.items.push(this.newItem());
  }
  removeItem(i: number): void {
    if (this.items.length > 1) this.items.removeAt(i);
  }

  onPriceSelect(index: number, priceId: string): void {
    if (!priceId) return;
    const price = this.servicePrices.find((p) => p.id === priceId);
    if (!price) return;
    this.items.at(index).patchValue({
      description: price.description,
      unitPrice: price.unitPrice,
      vatRate: price.vatRate,
    });
  }

  getLineTotal(index: number): number {
    const item = this.items.at(index).value;
    const unitPrice = this.roundMoney(Number(item.unitPrice) || 0);
    const base = this.roundMoney(unitPrice * (Number(item.quantity) || 1));
    const discount = this.roundMoney((base * (Number(item.discountPercent) || 0)) / 100);
    const net = this.roundMoney(base - discount);
    const vat = this.roundMoney((net * (Number(item.vatRate) || 0)) / 100);
    return this.roundMoney(net + vat);
  }

  getGrandTotal(): number {
    return this.roundMoney(
      this.items.controls.reduce((total, _, index) => total + this.getLineTotal(index), 0),
    );
  }

  loadPatients(): void {
    this.patientsLoading = true;
    this.patientService
      .findAll('', 0, 200)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (p) => {
          this.patients = p.content.map((x) => ({ id: x.id, fullName: x.fullName }));
          this.patientsLoading = false;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.patientsError = this.errorMessage(error, 'Erro ao carregar pacientes.');
          this.patientsLoading = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  loadPrices(): void {
    this.pricesLoading = true;
    this.financialService
      .findAllPrices()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (prices) => {
          this.servicePrices = prices;
          this.pricesLoading = false;
          this.changeDetectorRef.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.pricesError = this.errorMessage(error, 'Erro ao carregar tabela de preços.');
          this.pricesLoading = false;
          this.changeDetectorRef.markForCheck();
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

    const value = this.form.getRawValue();
    this.financialService
      .create({
        patientId: value.patientId,
        documentType: value.documentType,
        patientNif: value.patientNif || undefined,
        patientFiscalName: value.patientFiscalName || undefined,
        insuranceProvider: value.insuranceProvider || undefined,
        insurancePolicyNumber: value.insurancePolicyNumber || undefined,
        insuranceCoveragePercent: value.insuranceCoveragePercent,
        dueDate: value.dueDate || undefined,
        notes: value.notes || undefined,
        items: value.items.map((item) => ({
          description: item.description,
          quantity: item.quantity,
          unitPrice: item.unitPrice ?? undefined,
          discountPercent: item.discountPercent,
          vatRate: item.vatRate,
          servicePriceId: item.servicePriceId || undefined,
        })),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving = false;
          this.router.navigate(['/financial']);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao criar documento.');
          this.saving = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  goBack(): void {
    this.router.navigate(['/financial']);
  }
  get f() {
    return this.form.controls;
  }

  private roundMoney(amount: number): number {
    return Math.round((amount + Number.EPSILON) * 100) / 100;
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
