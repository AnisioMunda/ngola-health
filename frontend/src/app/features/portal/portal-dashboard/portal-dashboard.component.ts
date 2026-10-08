// ============================================================
// portal-dashboard.component.ts
// ============================================================
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  PortalService,
  PortalDashboardDto,
  PortalEpisodeDto,
  PortalLabResultDto,
  PortalPrescriptionDto,
  PortalInvoiceDto,
} from '../../../core/services/portal.service';
import { PortalDashboardHomeComponent } from './portal-dashboard-home.component';
import { PortalDashboardSidebarComponent } from './portal-dashboard-sidebar.component';
import { PortalEpisodesComponent } from './portal-episodes.component';
import { PortalInvoicesComponent } from './portal-invoices.component';
import { PortalLabResultsComponent } from './portal-lab-results.component';
import { PortalPrescriptionsComponent } from './portal-prescriptions.component';
import { PortalDashboardTab } from './portal-dashboard.types';

@Component({
  selector: 'app-portal-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    PortalDashboardSidebarComponent,
    PortalDashboardHomeComponent,
    PortalLabResultsComponent,
    PortalEpisodesComponent,
    PortalPrescriptionsComponent,
    PortalInvoicesComponent,
  ],
  // The shared dashboard stylesheet is scoped under .portal-layout for all child views.
  encapsulation: ViewEncapsulation.None,
  templateUrl: './portal-dashboard.component.html',
  styleUrls: ['./portal-dashboard.component.scss'],
})
export class PortalDashboardComponent implements OnInit {
  dashboard: PortalDashboardDto | null = null;
  prescriptions: PortalPrescriptionDto[] = [];
  invoices: PortalInvoiceDto[] = [];
  episodes: PortalEpisodeDto[] = [];
  labResults: PortalLabResultDto[] = [];
  labResultsLoaded = false;

  loading = true;
  error = '';

  activeTab: PortalDashboardTab = 'home';

  user = this.portalService.getCurrentUser();

  constructor(private portalService: PortalService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.portalService.getDashboard().subscribe({
      next: (d) => {
        this.dashboard = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar dados.';
        this.loading = false;
      },
    });
  }

  onTabChange(tab: PortalDashboardTab): void {
    this.activeTab = tab;
    this.error = '';
    if (tab === 'lab-results' && !this.labResultsLoaded) {
      this.portalService.getLabResults().subscribe({
        next: (results) => {
          this.labResults = results;
          this.labResultsLoaded = true;
        },
        error: () => {
          this.error = 'Erro ao carregar resultados laboratoriais.';
        },
      });
    }
    if (tab === 'prescriptions' && !this.prescriptions.length) {
      this.portalService.getPrescriptions().subscribe((p) => (this.prescriptions = p));
    }
    if (tab === 'invoices' && !this.invoices.length) {
      this.portalService.getInvoices().subscribe((i) => (this.invoices = i));
    }
    if (tab === 'episodes' && !this.episodes.length) {
      this.portalService.getEpisodes().subscribe((e) => (this.episodes = e));
    }
  }

  logout(): void {
    this.portalService.logout();
  }
}
