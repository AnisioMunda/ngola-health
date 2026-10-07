import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AdmissionResponse,
  BedResponse,
  DischargeCondition,
  DischargeRequest,
  InpatientService,
  TransferRequest,
  WardResponse,
  ADMISSION_STATUS_LABELS,
  DISCHARGE_CONDITION_LABELS,
} from '../../../core/services/inpatient.service';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admission-detail.component.html',
  styleUrls: ['./admission-detail.component.scss'],
})
export class AdmissionDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private bedLoadSequence = 0;
  private admissionLoadSequence = 0;
  private wardLoadSequence = 0;

  admission: AdmissionResponse | null = null;
  loading = true;
  error = '';
  success = '';

  showDischargeForm = false;
  dischargeNotes = '';
  dischargeCondition: DischargeCondition = 'IMPROVED';
  savingDischarge = false;

  showTransferForm = false;
  wards: WardResponse[] = [];
  selectedWardId = '';
  availableBeds: BedResponse[] = [];
  selectedBedId = '';
  transferReason = '';
  savingTransfer = false;
  loadingWards = false;
  loadingBeds = false;

  statusLabels = ADMISSION_STATUS_LABELS;
  conditionLabels = DISCHARGE_CONDITION_LABELS;

  dischargeConditions: { value: DischargeCondition; label: string }[] = [
    { value: 'IMPROVED', label: 'Melhorado' },
    { value: 'STABLE', label: 'Estável' },
    { value: 'CRITICAL', label: 'Crítico' },
    { value: 'DECEASED', label: 'Óbito' },
    { value: 'AGAINST_MEDICAL_ADVICE', label: 'Contra Indicação Médica' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private inpatientService: InpatientService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.load(id);
    } else {
      this.error = 'O identificador do internamento é inválido.';
      this.loading = false;
    }
  }

  load(id: string): void {
    const requestSequence = ++this.admissionLoadSequence;
    this.loading = true;
    this.error = '';
    this.inpatientService
      .findById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (admission) => {
          if (requestSequence !== this.admissionLoadSequence) return;
          this.admission = admission;
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.admissionLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar internamento.');
          this.admission = null;
          this.loading = false;
        },
      });
  }

  openDischargeForm(): void {
    if (!this.canDischarge() || this.savingDischarge) return;
    this.error = '';
    this.showTransferForm = false;
    this.showDischargeForm = true;
  }

  discharge(): void {
    if (!this.admission || !this.canDischarge() || this.savingDischarge) return;
    this.savingDischarge = true;
    this.error = '';
    const request: DischargeRequest = {
      dischargeNotes: this.dischargeNotes.trim() || null,
      dischargeCondition: this.dischargeCondition,
    };
    this.inpatientService
      .discharge(this.admission.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (admission) => {
          this.admission = admission;
          this.showDischargeForm = false;
          this.savingDischarge = false;
          this.flash('Alta registada com sucesso.');
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao dar alta.');
          this.savingDischarge = false;
        },
      });
  }

  openTransferForm(): void {
    if (!this.canTransfer() || this.savingTransfer) return;
    this.error = '';
    this.showDischargeForm = false;
    this.showTransferForm = true;
    this.selectedBedId = '';
    this.transferReason = '';
    this.loadTransferWards();
  }

  loadTransferWards(): void {
    const requestSequence = ++this.wardLoadSequence;
    this.loadingWards = true;
    this.inpatientService
      .findAllWards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (wards) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.wards = wards;
          this.loadingWards = false;
          const currentWardId = this.admission?.wardId;
          this.selectedWardId =
            wards.find((ward) => ward.id === currentWardId)?.id ?? wards[0]?.id ?? '';
          this.loadAvailableBeds(this.selectedWardId);
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar enfermarias.');
          this.loadingWards = false;
        },
      });
  }

  onTransferWardChange(): void {
    this.selectedBedId = '';
    this.loadAvailableBeds(this.selectedWardId);
  }

  loadAvailableBeds(wardId: string): void {
    const requestSequence = ++this.bedLoadSequence;
    this.selectedBedId = '';
    this.availableBeds = [];
    this.error = '';
    if (!this.admission || !wardId) {
      this.loadingBeds = false;
      return;
    }

    this.loadingBeds = true;
    this.inpatientService
      .findBedsByWard(wardId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (beds) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.availableBeds = beds.filter(
            (bed) =>
              bed.id !== this.admission?.bedId &&
              (bed.status === 'AVAILABLE' || bed.status === 'RESERVED'),
          );
          this.loadingBeds = false;
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar camas disponíveis.');
          this.loadingBeds = false;
        },
      });
  }

  closeTransferForm(): void {
    this.showTransferForm = false;
    this.selectedBedId = '';
  }

  transfer(): void {
    if (
      !this.admission ||
      !this.canTransfer() ||
      this.savingTransfer ||
      !this.availableBeds.some((bed) => bed.id === this.selectedBedId)
    ) {
      return;
    }
    this.savingTransfer = true;
    this.error = '';
    const request: TransferRequest = {
      toBedId: this.selectedBedId,
      reason: this.transferReason.trim() || null,
    };
    this.inpatientService
      .transfer(this.admission.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (admission) => {
          this.admission = admission;
          this.closeTransferForm();
          this.transferReason = '';
          this.savingTransfer = false;
          this.flash('Transferência realizada com sucesso.');
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro na transferência.');
          this.savingTransfer = false;
        },
      });
  }

  flash(message: string): void {
    this.success = message;
    setTimeout(() => (this.success = ''), 4000);
  }

  goBack(): void {
    this.router.navigate(['/inpatient/admissions']);
  }

  goToPatient(): void {
    if (this.admission) {
      this.router.navigate(['/patients', this.admission.patientId, 'edit']);
    }
  }

  canDischarge(): boolean {
    return this.admission !== null && this.admission.status === 'ACTIVE';
  }

  canTransfer(): boolean {
    return this.admission !== null && this.admission.status === 'ACTIVE';
  }

  isOverdue(): boolean {
    if (!this.admission?.expectedDischargeDate || this.admission.status !== 'ACTIVE') return false;
    const [year, month, day] = this.admission.expectedDischargeDate.split('-').map(Number);
    const expectedDate = new Date(year, month - 1, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expectedDate < today;
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
