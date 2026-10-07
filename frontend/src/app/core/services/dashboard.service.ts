import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MonthlyRevenue {
  month: string;
  yearMonth: string;
  revenue: number;
  paid: number;
  invoiceCount: number;
}

export interface MonthlyPatients {
  month: string;
  yearMonth: string;
  newPatients: number;
  totalEpisodes: number;
}

export interface MonthlyEpisodes {
  month: string;
  yearMonth: string;
  scheduled: number;
  completed: number;
  cancelled: number;
}

export interface ProvinceCount {
  province: string;
  count: number;
}

export interface RecentInvoice {
  id: string;
  invoiceNumber: string;
  patientName: string;
  totalAmount: number;
  status: string;
  createdAt: string;
}

export interface RecentPatient {
  id: string;
  fullName: string;
  createdAt: string;
}

export interface AdvancedDashboard {
  // Clínicos
  totalPatients: number;
  newPatientsThisMonth: number;
  newPatientsThisWeek: number;
  totalEpisodes: number;
  activeEpisodes: number;
  scheduledToday: number;
  completedToday: number;
  malePatients: number;
  femalePatients: number;
  // Financeiros
  revenueThisMonth: number;
  revenueLastMonth: number;
  revenueThisYear: number;
  pendingAmount: number;
  invoicesThisMonth: number;
  paidInvoicesThisMonth: number;
  pendingInvoices: number;
  collectionRate: number;
  // Lab / Farmácia
  labRequestsThisMonth: number;
  labRequestsPending: number;
  lowStockMedications: number;
  expiringMedications: number;
  // Tendências
  revenueByMonth: MonthlyRevenue[];
  patientsByMonth: MonthlyPatients[];
  episodesByMonth: MonthlyEpisodes[];
  // Distribuições
  episodesByType: Record<string, number>;
  usersByRole: Record<string, number>;
  patientsByProvince: ProvinceCount[];
  recentInvoices: RecentInvoice[];
  recentPatients: RecentPatient[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  getAdvanced(): Observable<AdvancedDashboard> {
    return this.http.get<AdvancedDashboard>(`${this.apiUrl}/advanced`);
  }
}