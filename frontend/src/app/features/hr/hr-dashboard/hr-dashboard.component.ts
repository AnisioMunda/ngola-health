import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  HrService,
  HrStatsDto,
  SHIFT_TYPE_COLORS,
  SHIFT_TYPE_LABELS,
} from '../../../core/services/hr.service';

@Component({
  selector: 'app-hr-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hr-dashboard.component.html',
  styleUrls: ['./hr-dashboard.component.scss'],
})
export class HrDashboardComponent implements OnInit {
  stats: HrStatsDto | null = null;
  loading = true;
  error = '';

  shiftTypeColors = SHIFT_TYPE_COLORS;
  shiftTypeLabels = SHIFT_TYPE_LABELS;

  constructor(
    private hrService: HrService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.hrService.getStats().subscribe({
      next: (s) => {
        this.stats = s;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar dados RH.';
        this.loading = false;
      },
    });
  }

  approveLeave(id: string): void {
    this.hrService.approveLeave(id, true).subscribe({
      next: () => this.load(),
    });
  }

  rejectLeave(id: string): void {
    const reason = prompt('Motivo da rejeição:');
    if (!reason) return;
    this.hrService.approveLeave(id, false, reason).subscribe({
      next: () => this.load(),
    });
  }

  goToShifts(): void {
    this.router.navigate(['/hr/shifts']);
  }
  goToLeaves(): void {
    this.router.navigate(['/hr/leaves']);
  }
  goToAttendance(): void {
    this.router.navigate(['/hr/attendance']);
  }

  getOccupancyRate(): number {
    if (!this.stats || this.stats.totalStaff === 0) return 0;
    return Math.round((this.stats.presentToday / this.stats.totalStaff) * 100);
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ').filter((p) => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
}
