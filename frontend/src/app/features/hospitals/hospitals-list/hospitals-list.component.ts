import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {
  HospitalManagementService,
  HospitalResponse,
  HospitalType,
} from '../../../core/services/hospital-management.service';

@Component({
  selector: 'app-hospitals-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hospitals-list.component.html',
  styleUrls: ['./hospitals-list.component.scss'],
})
export class HospitalsListComponent implements OnInit {
  hospitals: HospitalResponse[] = [];
  loading = true;
  error = '';

  constructor(
    private hospitalService: HospitalManagementService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadHospitals();
  }

  loadHospitals(): void {
    this.loading = true;
    this.error = '';
    this.hospitalService.findAllForManagement().subscribe({
      next: (hospitals) => {
        this.hospitals = hospitals;
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os hospitais. Tente novamente.';
        this.loading = false;
      },
    });
  }

  goToCreate(): void {
    void this.router.navigate(['/hospitals/new']);
  }

  goToEdit(id: string): void {
    void this.router.navigate(['/hospitals', id, 'edit']);
  }

  toggleStatus(hospital: HospitalResponse): void {
    const action = hospital.active
      ? this.hospitalService.deactivate(hospital.id)
      : this.hospitalService.activate(hospital.id);

    action.subscribe({
      next: (updated) => {
        const index = this.hospitals.findIndex((item) => item.id === updated.id);
        if (index !== -1) this.hospitals[index] = updated;
      },
      error: () => {
        this.error = 'Não foi possível alterar o estado do hospital.';
      },
    });
  }

  getTypeLabel(type: HospitalType): string {
    const labels: Record<HospitalType, string> = {
      HOSPITAL: 'Hospital',
      CLINIC: 'Clínica',
      POLYCLINIC: 'Policlínica',
      HEALTH_CENTER: 'Centro de saúde',
      LABORATORY: 'Laboratório',
    };
    return labels[type];
  }
}
