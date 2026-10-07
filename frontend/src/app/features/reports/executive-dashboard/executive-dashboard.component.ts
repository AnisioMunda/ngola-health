import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AdvancedReportsService,
  ExecutiveDashboardDto,
  BedOccupancyReportDto,
  FinancialReportDto,
  MonthlyDataPoint,
} from '../../../core/services/advanced-reports.service';
import { formatAoaCompactCurrency } from '../../../shared/utils/aoa-currency';

@Component({
  selector: 'app-executive-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './executive-dashboard.component.html',
  styleUrls: ['./executive-dashboard.component.scss'],
})
export class ExecutiveDashboardComponent implements OnInit {
  dashboard: ExecutiveDashboardDto | null = null;
  bedReport: BedOccupancyReportDto | null = null;
  financialReport: FinancialReportDto | null = null;

  loading = true;
  error = '';

  activeTab: 'executive' | 'financial' | 'beds' = 'executive';

  // Filtro financeiro
  financialFrom = new Date(new Date().getFullYear(), 0, 1).toISOString().split('T')[0];
  financialTo = new Date().toISOString().split('T')[0];

  constructor(
    private reportsService: AdvancedReportsService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.reportsService.getExecutiveDashboard().subscribe({
      next: (d) => {
        this.dashboard = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar relatórios.';
        this.loading = false;
      },
    });
  }

  loadBeds(): void {
    this.reportsService.getBedOccupancy().subscribe({
      next: (r) => {
        this.bedReport = r;
      },
    });
  }

  loadFinancial(): void {
    this.reportsService.getFinancialReport(this.financialFrom, this.financialTo).subscribe({
      next: (r) => {
        this.financialReport = r;
      },
    });
  }

  onTabChange(tab: 'executive' | 'financial' | 'beds'): void {
    this.activeTab = tab;
    if (tab === 'beds' && !this.bedReport) this.loadBeds();
    if (tab === 'financial' && !this.financialReport) this.loadFinancial();
  }

  // ------------------------------------------------
  // Helpers para os gráficos SVG inline
  // ------------------------------------------------

  getBarHeight(value: number, data: MonthlyDataPoint[]): number {
    const max = Math.max(...data.map((d) => Number(d.value)));
    return max === 0 ? 0 : (Number(value) / max) * 100;
  }

  getLinePoints(data: MonthlyDataPoint[], width: number, height: number): string {
    if (!data || data.length === 0) return '';
    const max = Math.max(...data.map((d) => Number(d.value)), 1);
    const step = width / (data.length - 1);
    return data
      .map((d, i) => {
        const x = i * step;
        const y = height - (Number(d.value) / max) * (height - 20) - 10;
        return `${x},${y}`;
      })
      .join(' ');
  }

  formatCurrency(val: number): string {
    return formatAoaCompactCurrency(val);
  }

  formatGrowth(val: number): string {
    return (val > 0 ? '+' : '') + val.toFixed(1) + '%';
  }

  // Cor para a barra de ocupação
  occupancyColor(rate: number): string {
    if (rate >= 90) return '#dc2626';
    if (rate >= 70) return '#f59e0b';
    return '#16a34a';
  }

  exportToCsv(type: string): void {
    let csv = '';
    let filename = '';

    if (type === 'revenue' && this.dashboard) {
      csv = 'Mês,Receita (Kz)\n';
      this.dashboard.revenueByMonth.forEach((d) => {
        csv += `${d.month},${d.value}\n`;
      });
      filename = 'receita_mensal.csv';
    } else if (type === 'beds' && this.bedReport) {
      csv = 'Enfermaria,Total Camas,Ocupadas,Taxa Ocupação\n';
      this.bedReport.byWard.forEach((w) => {
        csv += `${w.wardName},${w.totalBeds},${w.occupiedBeds},${w.occupancyRate.toFixed(1)}%\n`;
      });
      filename = 'ocupacao_camas.csv';
    }

    if (!csv) return;
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  goBack(): void {
    this.router.navigate(['/reports']);
  }

  getTopDoctorEpisodesData() {
    return (
      this.dashboard?.topDoctors?.map((x) => ({
        value: x.episodes,
        count: 0,
        month: '',
        monthKey: '',
      })) ?? []
    );
  }
}
