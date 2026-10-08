import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { PortalDashboardDto } from '../../../core/services/portal.service';
import { portalStatusColor } from './portal-dashboard.utils';

@Component({
  selector: 'app-portal-dashboard-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-dashboard-home.component.html',
})
export class PortalDashboardHomeComponent {
  @Input({ required: true }) dashboard!: PortalDashboardDto;

  statusColor = portalStatusColor;

  getPatientFirstName(): string {
    return this.dashboard.patientName?.trim().split(/\s+/)[0] ?? '';
  }
}
