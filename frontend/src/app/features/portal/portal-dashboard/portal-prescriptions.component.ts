import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { PortalPrescriptionDto } from '../../../core/services/portal.service';
import { portalStatusColor } from './portal-dashboard.utils';

@Component({
  selector: 'app-portal-prescriptions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-prescriptions.component.html',
})
export class PortalPrescriptionsComponent {
  @Input() prescriptions: PortalPrescriptionDto[] = [];

  statusColor = portalStatusColor;
}
