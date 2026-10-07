import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  InpatientService,
  WardResponse,
  WardMapResponse,
  BedResponse,
  EditableBedStatus,
  WARD_TYPE_LABELS,
  BED_STATUS_LABELS,
} from '../../../core/services/inpatient.service';

@Component({
  selector: 'app-ward-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ward-map.component.html',
  styleUrls: ['./ward-map.component.scss'],
})
export class WardMapComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private wardLoadSequence = 0;
  private mapLoadSequence = 0;

  wards: WardResponse[] = [];
  selectedWardId = '';
  wardMap: WardMapResponse | null = null;

  loading = true;
  loadingMap = false;
  updatingBedId: string | null = null;
  error = '';
  success = '';

  wardTypeLabels = WARD_TYPE_LABELS;
  bedStatusLabels = BED_STATUS_LABELS;

  // Modal de admissão rápida
  showAdmitModal = false;
  showStatusModal = false;
  selectedBed: BedResponse | null = null;

  constructor(
    private inpatientService: InpatientService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadWards();
  }

  loadWards(): void {
    const requestSequence = ++this.wardLoadSequence;
    this.loading = true;
    this.error = '';
    this.inpatientService
      .findAllWards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (wards) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.wards = wards;
          this.loading = false;
          const selectedWard = wards.find((ward) => ward.id === this.selectedWardId) ?? wards[0];
          this.selectedWardId = selectedWard?.id ?? '';
          if (selectedWard) {
            this.loadMap();
          } else {
            this.mapLoadSequence++;
            this.wardMap = null;
            this.loadingMap = false;
          }
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar enfermarias.');
          this.wards = [];
          this.selectedWardId = '';
          this.wardMap = null;
          this.mapLoadSequence++;
          this.loading = false;
          this.loadingMap = false;
        },
      });
  }

  loadMap(): void {
    const requestSequence = ++this.mapLoadSequence;
    const wardId = this.selectedWardId;
    if (!wardId) {
      this.wardMap = null;
      this.loadingMap = false;
      return;
    }
    this.loadingMap = true;
    this.wardMap = null;
    this.error = '';
    this.inpatientService
      .getWardMap(wardId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (map) => {
          if (requestSequence !== this.mapLoadSequence || wardId !== this.selectedWardId) return;
          this.wardMap = map;
          this.loadingMap = false;
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.mapLoadSequence || wardId !== this.selectedWardId) return;
          this.error = this.errorMessage(error, 'Erro ao carregar mapa.');
          this.loadingMap = false;
        },
      });
  }

  onWardChange(): void {
    this.loadMap();
  }

  selectBed(bed: BedResponse): void {
    this.selectedBed = bed;
    if (bed.status === 'AVAILABLE') {
      this.showAdmitModal = true;
    } else if (bed.status === 'OCCUPIED' && bed.admissionId) {
      this.router.navigate(['/inpatient/admissions', bed.admissionId]);
    } else if (bed.status === 'OCCUPIED') {
      this.error = 'Não foi possível localizar o internamento associado a esta cama.';
    } else {
      this.showStatusModal = true;
    }
  }

  setBedStatus(status: EditableBedStatus): void {
    if (!this.selectedBed || this.updatingBedId) return;
    const bedId = this.selectedBed.id;
    this.updatingBedId = bedId;
    this.error = '';
    this.inpatientService
      .updateBedStatus(bedId, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.updatingBedId = null;
          this.closeModal();
          this.flash('Estado da cama actualizado.');
          this.loadWards();
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao actualizar cama.');
          this.updatingBedId = null;
        },
      });
  }

  goToAdmit(): void {
    const bedId = this.selectedBed?.id;
    this.closeModal();
    if (bedId) {
      this.router.navigate(['/inpatient/admissions/new'], {
        queryParams: { bedId },
      });
    }
  }

  goToAdmissions(): void {
    this.router.navigate(['/inpatient/admissions']);
  }
  goToSetup(): void {
    this.router.navigate(['/inpatient/setup']);
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3000);
  }

  get occupancyRate(): number {
    if (!this.wardMap || this.wardMap.totalBeds === 0) return 0;
    return Math.round((this.wardMap.occupiedBeds / this.wardMap.totalBeds) * 100);
  }

  get selectedWard(): WardResponse | undefined {
    return this.wards.find((w) => w.id === this.selectedWardId);
  }

  getDaysAgo(dateStr: string): number {
    const diff = Date.now() - new Date(dateStr).getTime();
    return Math.max(0, Math.floor(diff / 86400000));
  }

  getBedAriaLabel(bed: BedResponse): string {
    const occupant = bed.patientName ? `, paciente ${bed.patientName}` : '';
    return `Cama ${bed.bedNumber}, ${this.bedStatusLabels[bed.status]}${occupant}`;
  }

  closeModal(): void {
    this.showAdmitModal = false;
    this.showStatusModal = false;
    this.selectedBed = null;
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
