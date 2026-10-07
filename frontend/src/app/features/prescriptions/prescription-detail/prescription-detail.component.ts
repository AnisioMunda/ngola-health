import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PrescriptionService,
  PrescriptionResponse,
  PrescriptionItemResponse,
  PRESCRIPTION_STATUS_LABELS,
  ITEM_STATUS_LABELS,
  STATUS_COLORS,
} from '../../../core/services/prescription.service';

@Component({
  selector: 'app-prescription-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prescription-detail.component.html',
  styleUrls: ['./prescription-detail.component.scss'],
})
export class PrescriptionDetailComponent implements OnInit {
  prescription: PrescriptionResponse | null = null;
  loading = true;
  error = '';
  success = '';

  // Dispensa
  showDispenseForm = false;
  selectedItem: PrescriptionItemResponse | null = null;
  quantityToDispense = 1;
  dispenseNotes = '';
  savingDispense = false;

  // Cancelamento
  showCancelForm = false;
  cancelReason = '';
  savingCancel = false;

  statusLabels = PRESCRIPTION_STATUS_LABELS;
  itemLabels = ITEM_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prescriptionService: PrescriptionService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading = true;
    this.prescriptionService.findById(id).subscribe({
      next: (p) => {
        this.prescription = p;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar prescrição.';
        this.loading = false;
      },
    });
  }

  openDispense(item: PrescriptionItemResponse): void {
    this.selectedItem = item;
    this.quantityToDispense = item.remainingQuantity;
    this.dispenseNotes = '';
    this.showDispenseForm = true;
    this.showCancelForm = false;
  }

  dispense(): void {
    if (!this.prescription || !this.selectedItem) return;
    if (this.quantityToDispense < 1) {
      this.error = 'Quantidade deve ser pelo menos 1.';
      return;
    }
    this.savingDispense = true;
    this.prescriptionService
      .dispense(this.prescription.id, {
        prescriptionItemId: this.selectedItem.id,
        quantityToDispense: this.quantityToDispense,
        notes: this.dispenseNotes || null,
      })
      .subscribe({
        next: (p) => {
          this.prescription = p;
          this.showDispenseForm = false;
          this.savingDispense = false;
          this.flash('Medicamento dispensado com sucesso.');
        },
        error: (err) => {
          this.error = err.error?.message ?? 'Erro ao dispensar.';
          this.savingDispense = false;
        },
      });
  }

  cancel(): void {
    if (!this.prescription || !this.cancelReason) return;
    this.savingCancel = true;
    this.prescriptionService.cancel(this.prescription.id, this.cancelReason).subscribe({
      next: (p) => {
        this.prescription = p;
        this.showCancelForm = false;
        this.savingCancel = false;
        this.flash('Prescrição cancelada.');
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao cancelar.';
        this.savingCancel = false;
      },
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 4000);
  }

  goBack(): void {
    this.router.navigate(['/prescriptions']);
  }
  goToPatient(): void {
    if (this.prescription) this.router.navigate(['/patients', this.prescription.patientId, 'edit']);
  }

  canDispense(): boolean {
    return (
      this.prescription?.status === 'ACTIVE' || this.prescription?.status === 'PARTIALLY_DISPENSED'
    );
  }

  canCancel(): boolean {
    return (
      this.prescription?.status === 'ACTIVE' || this.prescription?.status === 'PARTIALLY_DISPENSED'
    );
  }

  daysUntilExpiry(): number {
    if (!this.prescription) return 0;
    return Math.ceil((new Date(this.prescription.expiryDate).getTime() - Date.now()) / 86400000);
  }

  dispensedTotal(): number {
    return this.prescription?.dispensations?.reduce((s, d) => s + d.quantityDispensed, 0) ?? 0;
  }

  progressPercent(item: PrescriptionItemResponse): number {
    return Math.round((item.quantityDispensed / item.quantityPrescribed) * 100);
  }
}
