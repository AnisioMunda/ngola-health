import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  InpatientService,
  WardResponse,
  WardMapResponse,
  BedResponse,
  BedStatus,
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
  wards: WardResponse[] = [];
  selectedWardId = '';
  wardMap: WardMapResponse | null = null;

  loading = true;
  loadingMap = false;
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
    this.loading = true;
    this.inpatientService.findAllWards().subscribe({
      next: (wards) => {
        this.wards = wards;
        this.loading = false;
        if (wards.length > 0) {
          this.selectedWardId = wards[0].id;
          this.loadMap();
        }
      },
      error: () => {
        this.error = 'Erro ao carregar enfermarias.';
        this.loading = false;
      },
    });
  }

  loadMap(): void {
    if (!this.selectedWardId) return;
    this.loadingMap = true;
    this.wardMap = null;
    this.inpatientService.getWardMap(this.selectedWardId).subscribe({
      next: (map) => {
        this.wardMap = map;
        this.loadingMap = false;
      },
      error: () => {
        this.error = 'Erro ao carregar mapa.';
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
    } else {
      this.showStatusModal = true;
    }
  }

  setBedStatus(status: BedStatus): void {
    if (!this.selectedBed) return;
    this.inpatientService.updateBedStatus(this.selectedBed.id, status).subscribe({
      next: () => {
        this.showStatusModal = false;
        this.flash('Estado da cama actualizado.');
        this.loadMap();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao actualizar cama.';
      },
    });
  }

  goToAdmit(): void {
    this.showAdmitModal = false;
    if (this.selectedBed) {
      this.router.navigate(['/inpatient/admissions/new'], {
        queryParams: { bedId: this.selectedBed.id },
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

  bedColor(status: BedStatus): string {
    return (
      {
        AVAILABLE: '#16a34a',
        OCCUPIED: '#dc2626',
        MAINTENANCE: '#f59e0b',
        RESERVED: '#3b82f6',
      }[status] ?? '#9ca3af'
    );
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
    return Math.floor(diff / 86400000);
  }
}
