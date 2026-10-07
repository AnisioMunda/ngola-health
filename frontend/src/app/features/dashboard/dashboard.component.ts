import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DashboardService, AdvancedDashboard } from '../../core/services/dashboard.service';
import { formatAoaCompactCurrency, formatAoaCurrency } from '../../shared/utils/aoa-currency';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('patientsChart') patientsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('episodesTypeChart') episodesTypeChartRef!: ElementRef<HTMLCanvasElement>;

  data: AdvancedDashboard | null = null;
  loading = true;
  error = '';

  private revenueChart?: Chart;
  private patientsChart?: Chart;
  private episodesTypeChart?: Chart;

  readonly STATUS_LABELS: Record<string, string> = {
    RASCUNHO: 'Rascunho',
    EMITIDO: 'Emitido',
    PAGO_PARCIALMENTE: 'Pago Parc.',
    PAGO: 'Pago',
    ANULADO: 'Anulado',
    EM_ATRASO: 'Em Atraso',
  };

  readonly EPISODE_TYPE_LABELS: Record<string, string> = {
    OUTPATIENT: 'Ambulatório',
    EMERGENCY: 'Urgência',
    INPATIENT: 'Internamento',
    EXAM: 'Exame',
    SURGERY: 'Cirurgia',
  };

  constructor(
    private dashboardService: DashboardService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    if (this.data) this.buildCharts();
  }

  ngOnDestroy(): void {
    this.revenueChart?.destroy();
    this.patientsChart?.destroy();
    this.episodesTypeChart?.destroy();
  }

  load(): void {
    this.loading = true;
    this.dashboardService.getAdvanced().subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
        setTimeout(() => this.buildCharts(), 50);
      },
      error: () => {
        this.error = 'Erro ao carregar dashboard.';
        this.loading = false;
      },
    });
  }

  buildCharts(): void {
    if (!this.data) return;
    this.buildRevenueChart();
    this.buildPatientsChart();
    this.buildEpisodesTypeChart();
  }

  // ------------------------------------------------
  // Gráfico de receita mensal (barras)
  // ------------------------------------------------
  buildRevenueChart(): void {
    if (!this.revenueChartRef || !this.data?.revenueByMonth?.length) return;
    this.revenueChart?.destroy();

    const labels = this.data.revenueByMonth.map((r) => r.month);
    const revenue = this.data.revenueByMonth.map((r) => r.revenue);
    const paid = this.data.revenueByMonth.map((r) => r.paid);

    this.revenueChart = new Chart(this.revenueChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          {
            label: 'Facturado',
            data: revenue,
            backgroundColor: 'rgba(32, 58, 67, 0.15)',
            borderColor: '#203a43',
            borderWidth: 2,
            borderRadius: 6,
          },
          {
            label: 'Recebido',
            data: paid,
            backgroundColor: 'rgba(22, 163, 74, 0.8)',
            borderColor: '#16a34a',
            borderWidth: 0,
            borderRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'top', labels: { font: { size: 11 } } },
          tooltip: {
            callbacks: {
              label: (ctx) => `${ctx.dataset.label}: ${this.formatKz(ctx.raw as number)}`,
            },
          },
        },
        scales: {
          y: {
            ticks: {
              callback: (val) => this.formatKzShort(val as number),
              font: { size: 10 },
            },
            grid: { color: '#f1f5f9' },
          },
          x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        },
      },
    });
  }

  // ------------------------------------------------
  // Gráfico de pacientes mensais (linha)
  // ------------------------------------------------
  buildPatientsChart(): void {
    if (!this.patientsChartRef || !this.data?.patientsByMonth?.length) return;
    this.patientsChart?.destroy();

    const labels = this.data.patientsByMonth.map((p) => p.month);
    const patients = this.data.patientsByMonth.map((p) => p.newPatients);
    const episodes = this.data.patientsByMonth.map((p) => p.totalEpisodes);

    this.patientsChart = new Chart(this.patientsChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Novos Pacientes',
            data: patients,
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            tension: 0.4,
            fill: true,
            pointRadius: 4,
            pointBackgroundColor: '#3b82f6',
          },
          {
            label: 'Episódios',
            data: episodes,
            borderColor: '#f59e0b',
            backgroundColor: 'rgba(245, 158, 11, 0.08)',
            tension: 0.4,
            fill: true,
            pointRadius: 4,
            pointBackgroundColor: '#f59e0b',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', labels: { font: { size: 11 } } } },
        scales: {
          y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { font: { size: 10 } } },
          x: { grid: { display: false }, ticks: { font: { size: 11 } } },
        },
      },
    });
  }

  // ------------------------------------------------
  // Gráfico de episódios por tipo (rosca)
  // ------------------------------------------------
  buildEpisodesTypeChart(): void {
    if (!this.episodesTypeChartRef || !this.data?.episodesByType) return;
    this.episodesTypeChart?.destroy();

    const entries = Object.entries(this.data.episodesByType);
    if (!entries.length) return;

    const labels = entries.map(([k]) => this.EPISODE_TYPE_LABELS[k] ?? k);
    const values = entries.map(([, v]) => v);
    const colors = ['#203a43', '#3b82f6', '#16a34a', '#f59e0b', '#8b5cf6'];

    this.episodesTypeChart = new Chart(this.episodesTypeChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [
          {
            data: values,
            backgroundColor: colors,
            borderWidth: 2,
            borderColor: '#ffffff',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { font: { size: 11 }, padding: 12 } },
        },
        cutout: '65%',
      },
    });
  }

  // ------------------------------------------------
  // Helpers
  // ------------------------------------------------

  get revenueGrowth(): number {
    if (!this.data || !this.data.revenueLastMonth) return 0;
    return Math.round(
      ((this.data.revenueThisMonth - this.data.revenueLastMonth) / this.data.revenueLastMonth) *
        100,
    );
  }

  formatKz(val: number): string {
    return formatAoaCurrency(val);
  }

  formatKzShort(val: number): string {
    return formatAoaCompactCurrency(val);
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }
  objectKeys = Object.keys;
}
