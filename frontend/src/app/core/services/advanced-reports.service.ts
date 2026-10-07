import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MonthlyDataPoint {
  month: string;
  monthKey: string;
  value: number;
  count: number;
}

export interface CategoryDataPoint {
  label: string;
  count: number;
  percentage: number;
  color: string;
}

export interface DoctorStats {
  doctorName: string;
  episodes: number;
  appointments: number;
  prescriptions: number;
}

export interface ExecutiveDashboardDto {
  revenueThisMonth: number;
  revenueLastMonth: number;
  revenueGrowthPercent: number;
  revenueYTD: number;
  invoicesThisMonth: number;
  invoicesPending: number;
  totalPatients: number;
  newPatientsThisMonth: number;
  episodesToday: number;
  episodesThisMonth: number;
  admissionsActive: number;
  bedsAvailable: number;
  bedsTotal: number;
  occupancyRate: number;
  appointmentsToday: number;
  pendingLeaves: number;
  revenueByMonth: MonthlyDataPoint[];
  patientsByMonth: MonthlyDataPoint[];
  episodesByMonth: MonthlyDataPoint[];
  topDoctors: DoctorStats[];
  triageByPriority: CategoryDataPoint[];
}

export interface WardOccupancy {
  wardName: string;
  wardType: string;
  totalBeds: number;
  occupiedBeds: number;
  occupancyRate: number;
  avgStayDays: number;
}

export interface BedOccupancyReportDto {
  totalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  occupancyRate: number;
  avgStayDays: number;
  byWard: WardOccupancy[];
}

export interface FinancialReportDto {
  period: string;
  totalRevenue: number;
  paidRevenue: number;
  pendingRevenue: number;
  totalInvoices: number;
  paidInvoices: number;
  pendingInvoices: number;
  revenueByMonth: MonthlyDataPoint[];
}

@Injectable({ providedIn: 'root' })
export class AdvancedReportsService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reports/advanced`;

  getExecutiveDashboard(): Observable<ExecutiveDashboardDto> {
    return this.http.get<ExecutiveDashboardDto>(`${this.apiUrl}/executive`);
  }

  getBedOccupancy(): Observable<BedOccupancyReportDto> {
    return this.http.get<BedOccupancyReportDto>(`${this.apiUrl}/bed-occupancy`);
  }

  getFinancialReport(from?: string, to?: string): Observable<FinancialReportDto> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to)   params = params.set('to',   to);
    return this.http.get<FinancialReportDto>(`${this.apiUrl}/financial`, { params });
  }
}