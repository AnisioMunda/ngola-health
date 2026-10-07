import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  FinancialService,
  ServicePriceResponse,
  DocumentType,
} from '../../../core/services/financial.service';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './invoice-form.component.html',
  styleUrls: ['./invoice-form.component.scss'],
})
export class InvoiceFormComponent implements OnInit {
  form!: FormGroup;
  saving = false;
  error = '';

  patients: { id: string; fullName: string }[] = [];
  servicePrices: ServicePriceResponse[] = [];

  docTypes: { value: DocumentType; label: string; desc: string }[] = [
    { value: 'FR', label: 'FR — Factura/Recibo', desc: 'Com pagamento no acto' },
    { value: 'FT', label: 'FT — Factura', desc: 'Sem pagamento imediato' },
    { value: 'NC', label: 'NC — Nota de Crédito', desc: 'Devolução / anulação' },
    { value: 'ND', label: 'ND — Nota de Débito', desc: 'Cobrança adicional' },
  ];

  constructor(
    private fb: FormBuilder,
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
    this.form = this.fb.group({
      patientId: ['', Validators.required],
      documentType: ['FR', Validators.required],
      patientNif: [''],
      patientFiscalName: [''],
      insuranceProvider: [''],
      notes: [''],
      items: this.fb.array([this.newItem()]),
    });
  }

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  newItem(): FormGroup {
    return this.fb.group({
      servicePriceId: [''],
      description: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: [null, [Validators.required, Validators.min(0.01)]],
      discountPercent: [0],
      vatRate: [0],
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
    const base = (item.unitPrice || 0) * (item.quantity || 1);
    const disc = (base * (item.discountPercent || 0)) / 100;
    const afterDisc = base - disc;
    const vat = (afterDisc * (item.vatRate || 0)) / 100;
    return afterDisc + vat;
  }

  getGrandTotal(): number {
    let total = 0;
    for (let i = 0; i < this.items.length; i++) total += this.getLineTotal(i);
    return total;
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => {
        this.patients = p.content.map((x) => ({ id: x.id, fullName: x.fullName }));
      },
    });
  }

  loadPrices(): void {
    this.financialService.findAllPrices().subscribe({
      next: (prices) => {
        this.servicePrices = prices;
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
    this.financialService
      .create({
        ...value,
        items: value.items.map((i: any) => ({
          ...i,
          servicePriceId: i.servicePriceId || undefined,
        })),
      })
      .subscribe({
        next: () => this.router.navigate(['/financial']),
        error: (err) => {
          this.error = err.error?.message ?? 'Erro ao criar documento.';
          this.saving = false;
        },
      });
  }

  goBack(): void {
    this.router.navigate(['/financial']);
  }
  get f() {
    return this.form.controls;
  }
}
