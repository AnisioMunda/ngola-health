import { Component, OnDestroy, OnInit } from '@angular/core';
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
  prescriptionErrorMessage,
} from '../../../core/services/prescription.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-prescription-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prescription-detail.component.html',
  styleUrls: ['./prescription-detail.component.scss'],
})
export class PrescriptionDetailComponent implements OnInit, OnDestroy {
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
  private successTimeout: ReturnType<typeof setTimeout> | null = null;

  statusLabels = PRESCRIPTION_STATUS_LABELS;
  itemLabels = ITEM_STATUS_LABELS;
  statusColors = STATUS_COLORS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prescriptionService: PrescriptionService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(id);
    } else {
      this.error = 'Não foi indicado o identificador da prescrição.';
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    if (this.successTimeout) clearTimeout(this.successTimeout);
  }

  load(id: string): void {
    this.loading = true;
    this.prescriptionService.findById(id).subscribe({
      next: (p) => {
        this.prescription = p;
        this.error = '';
        this.loading = false;
      },
      error: (error: unknown) => {
        this.error = prescriptionErrorMessage(error, 'Não foi possível carregar a prescrição.');
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
    if (!this.prescription || !this.selectedItem || !this.canDispense()) return;
    if (
      !Number.isInteger(this.quantityToDispense) ||
      this.quantityToDispense < 1 ||
      this.quantityToDispense > this.selectedItem.remainingQuantity ||
      this.quantityToDispense > this.selectedItem.stockAvailable
    ) {
      this.error = 'A quantidade deve estar dentro do stock disponível e da quantidade pendente.';
      return;
    }
    this.savingDispense = true;
    this.error = '';
    this.prescriptionService
      .dispense(this.prescription.id, {
        prescriptionItemId: this.selectedItem.id,
        quantityToDispense: this.quantityToDispense,
        notes: this.dispenseNotes.trim() || null,
      })
      .subscribe({
        next: (p) => {
          this.prescription = p;
          this.showDispenseForm = false;
          this.savingDispense = false;
          this.flash('Medicamento dispensado com sucesso.');
        },
        error: (error: unknown) => {
          this.error = prescriptionErrorMessage(error, 'Não foi possível dispensar o medicamento.');
          this.savingDispense = false;
        },
      });
  }

  cancel(): void {
    const reason = this.cancelReason.trim();
    if (!this.prescription || !this.canCancel()) return;
    if (!reason) {
      this.error = 'Indique o motivo do cancelamento.';
      return;
    }
    if (reason.length > 300) {
      this.error = 'O motivo do cancelamento não pode exceder 300 caracteres.';
      return;
    }
    this.savingCancel = true;
    this.error = '';
    this.prescriptionService.cancel(this.prescription.id, reason).subscribe({
      next: (p) => {
        this.prescription = p;
        this.showCancelForm = false;
        this.savingCancel = false;
        this.flash('Prescrição cancelada.');
      },
      error: (error: unknown) => {
        this.error = prescriptionErrorMessage(error, 'Não foi possível cancelar a prescrição.');
        this.savingCancel = false;
      },
    });
  }

  flash(msg: string): void {
    if (this.successTimeout) clearTimeout(this.successTimeout);
    this.success = msg;
    this.successTimeout = setTimeout(() => {
      this.success = '';
      this.successTimeout = null;
    }, 4000);
  }

  goBack(): void {
    this.router.navigate(['/prescriptions']);
  }
  goToPatient(): void {
    if (this.prescription) this.router.navigate(['/patients', this.prescription.patientId, 'edit']);
  }

  canDispense(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      (roles.includes('ADMIN') || roles.includes('PHARMACIST')) &&
      !this.prescription?.expired &&
      (this.prescription?.status === 'ACTIVE' ||
        this.prescription?.status === 'PARTIALLY_DISPENSED')
    );
  }

  canCancel(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      (roles.includes('ADMIN') || roles.includes('DOCTOR') || roles.includes('MANAGER')) &&
      (this.prescription?.status === 'ACTIVE' ||
        this.prescription?.status === 'PARTIALLY_DISPENSED')
    );
  }

  daysUntilExpiry(): number {
    if (!this.prescription) return 0;
    const today = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Africa/Luanda',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());
    const todayAtUtc = Date.parse(`${today}T00:00:00Z`);
    const expiryAtUtc = Date.parse(`${this.prescription.expiryDate}T00:00:00Z`);
    return Math.ceil((expiryAtUtc - todayAtUtc) / 86400000);
  }

  dispensedTotal(): number {
    return this.prescription?.dispensations?.reduce((s, d) => s + d.quantityDispensed, 0) ?? 0;
  }

  progressPercent(item: PrescriptionItemResponse): number {
    return Math.round((item.quantityDispensed / item.quantityPrescribed) * 100);
  }
}
