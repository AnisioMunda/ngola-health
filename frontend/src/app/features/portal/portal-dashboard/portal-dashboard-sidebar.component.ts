import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PortalDashboardDto, PortalLoginResponse } from '../../../core/services/portal.service';
import { PortalDashboardTab } from './portal-dashboard.types';

@Component({
  selector: 'app-portal-dashboard-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-dashboard-sidebar.component.html',
})
export class PortalDashboardSidebarComponent {
  @Input() user: PortalLoginResponse | null = null;
  @Input() dashboard: PortalDashboardDto | null = null;
  @Input() activeTab: PortalDashboardTab = 'home';

  @Output() tabChange = new EventEmitter<PortalDashboardTab>();
  @Output() logout = new EventEmitter<void>();

  getInitials(): string {
    const name = this.user?.patientName?.trim();
    if (!name) return 'P';
    const parts = name.split(/\s+/);
    return parts.length > 1 ? (parts[0][0] + parts[1][0]).toUpperCase() : parts[0][0].toUpperCase();
  }
}
