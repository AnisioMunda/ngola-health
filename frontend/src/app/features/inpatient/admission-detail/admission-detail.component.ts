import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  InpatientService, AdmissionResponse, BedResponse,
  DischargeCondition, ADMISSION_STATUS_LABELS,
  DISCHARGE_CONDITION_LABELS
} from '../../../core/services/inpatient.service';

@Component({
  selector: 'app-admission-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admission-detail.component.html',
  styleUrls: ['./admission-detail.component.scss']
})
export class AdmissionDetailComponent implements OnInit {

  admission: AdmissionResponse | null = null;
  loading  = true;
  error    = '';
  success  = '';

  // Alta
  showDischargeForm   = false;
  dischargeNotes      = '';
  dischargeCondition: DischargeCondition = 'IMPROVED';
  savingDischarge     = false;

  // Transferência
  showTransferForm    = false;
  availableBeds: BedResponse[] = [];
  selectedBedId       = '';
  transferReason      = '';
  savingTransfer      = false;
  loadingBeds         = false;

  statusLabels    = ADMISSION_STATUS_LABELS;
  conditionLabels = DISCHARGE_CONDITION_LABELS;

  dischargeConditions: { value: DischargeCondition; label: string }[] = [
    { value: 'IMPROVED',               label: 'Melhorado'                },
    { value: 'STABLE',                 label: 'Estável'                  },
    { value: 'CRITICAL',               label: 'Crítico'                  },
    { value: 'DECEASED',               label: 'Óbito'                    },
    { value: 'AGAINST_MEDICAL_ADVICE', label: 'Contra Indicação Médica'  }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private inpatientService: InpatientService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading = true;
    this.inpatientService.findById(id).subscribe({
      next: (a) => { this.admission = a; this.loading = false; },
      error: () => { this.error = 'Erro ao carregar internamento.'; this.loading = false; }
    });
  }

  discharge(): void {
    if (!this.admission) return;
    this.savingDischarge = true;
    this.inpatientService.discharge(this.admission.id, {
      dischargeNotes:     this.dischargeNotes,
      dischargeCondition: this.dischargeCondition
    }).subscribe({
      next: (a) => {
        this.admission       = a;
        this.showDischargeForm = false;
        this.savingDischarge   = false;
        this.flash('Alta registada com sucesso.');
      },
      error: (err) => {
        this.error          = err.error?.message ?? 'Erro ao dar alta.';
        this.savingDischarge = false;
      }
    });
  }

  loadAvailableBeds(): void {
    if (!this.admission) return;
    this.loadingBeds  = true;
    this.availableBeds = [];
    this.inpatientService.findBedsByWard(this.admission.wardId).subscribe({
      next: (beds) => {
        this.availableBeds = beds.filter(b =>
          b.status === 'AVAILABLE' && b.id !== this.admission!.bedId);
        this.loadingBeds = false;
      },
      error: () => { this.loadingBeds = false; }
    });
  }

  openTransferForm(): void {
    this.showTransferForm = true;
    this.loadAvailableBeds();
  }

  transfer(): void {
    if (!this.admission || !this.selectedBedId) return;
    this.savingTransfer = true;
    this.inpatientService.transfer(this.admission.id, {
      toBedId: this.selectedBedId,
      reason:  this.transferReason
    }).subscribe({
      next: (a) => {
        this.admission      = a;
        this.showTransferForm = false;
        this.selectedBedId   = '';
        this.transferReason  = '';
        this.savingTransfer  = false;
        this.flash('Transferência realizada com sucesso.');
      },
      error: (err) => {
        this.error         = err.error?.message ?? 'Erro na transferência.';
        this.savingTransfer = false;
      }
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 4000);
  }

  goBack(): void { this.router.navigate(['/inpatient/admissions']); }
  goToPatient(): void {
    if (this.admission)
      this.router.navigate(['/patients', this.admission.patientId, 'edit']);
  }

  canDischarge(): boolean { return this.admission?.status === 'ACTIVE'; }
  canTransfer():  boolean { return this.admission?.status === 'ACTIVE'; }

  isOverdue(): boolean {
    if (!this.admission?.expectedDischargeDate || this.admission.status !== 'ACTIVE') return false;
    return new Date(this.admission.expectedDischargeDate) < new Date();
  }
}