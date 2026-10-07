import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  HrService, AttendanceResponse,
  ATTENDANCE_STATUS_LABELS
} from '../../../core/services/hr.service';

@Component({
  selector: 'app-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './attendance.component.html',
  styleUrls: ['./attendance.component.scss']
})
export class AttendanceComponent implements OnInit {

  myAttendance:    AttendanceResponse[] = [];
  dailyAttendance: AttendanceResponse[] = [];
  todayRecord:     AttendanceResponse | null = null;

  loading       = true;
  saving        = false;
  error         = '';
  success       = '';

  activeTab: 'daily' | 'mine' = 'daily';

  // Filtro período
  fromDate = this.firstDayOfMonth();
  toDate   = new Date().toISOString().split('T')[0];
  dailyDate = new Date().toISOString().split('T')[0];

  statusLabels = ATTENDANCE_STATUS_LABELS;

  constructor(
    private hrService: HrService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDailyAttendance();
    this.loadMyAttendance();
    this.loadTodayRecord();
  }

  loadDailyAttendance(): void {
    this.hrService.getDailyAttendance(this.dailyDate).subscribe({
      next: (a) => { this.dailyAttendance = a; this.loading = false; },
      error: () => { this.error = 'Erro ao carregar presenças.'; this.loading = false; }
    });
  }

  loadMyAttendance(): void {
    this.hrService.getMyAttendance(this.fromDate, this.toDate).subscribe({
      next: (a) => { this.myAttendance = a; }
    });
  }

  loadTodayRecord(): void {
    const today = new Date().toISOString().split('T')[0];
    this.hrService.getMyAttendance(today, today).subscribe({
      next: (a) => { this.todayRecord = a.length > 0 ? a[0] : null; }
    });
  }

  checkIn(): void {
    this.saving = true;
    this.hrService.checkIn().subscribe({
      next: (a) => {
        this.todayRecord = a;
        this.saving = false;
        this.flash('Check-in registado com sucesso.');
        this.loadDailyAttendance();
      },
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao registar check-in.';
        this.saving = false;
      }
    });
  }

  checkOut(): void {
    this.saving = true;
    this.hrService.checkOut().subscribe({
      next: (a) => {
        this.todayRecord = a;
        this.saving = false;
        this.flash('Check-out registado com sucesso.');
        this.loadDailyAttendance();
        this.loadMyAttendance();
      },
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao registar check-out.';
        this.saving = false;
      }
    });
  }

  onDateChange(): void { this.loadDailyAttendance(); }
  onPeriodChange(): void { this.loadMyAttendance(); }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  totalHoursThisMonth(): string {
    const total = this.myAttendance.reduce(
      (sum, a) => sum + (a.minutesWorked ?? 0), 0);
    return Math.floor(total / 60) + 'h ' + (total % 60) + 'min';
  }

  totalOvertimeThisMonth(): string {
    const total = this.myAttendance.reduce(
      (sum, a) => sum + (a.overtimeMinutes ?? 0), 0);
    return Math.floor(total / 60) + 'h ' + (total % 60) + 'min';
  }

  presentDays(): number {
    return this.myAttendance.filter(a => a.status === 'PRESENT' || a.status === 'LATE').length;
  }

  firstDayOfMonth(): string {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().split('T')[0];
  }

  formatTime(dt: string): string {
    if (!dt) return '—';
    return new Date(dt).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' });
  }

  goBack(): void { this.router.navigate(['/hr']); }

  get hasCheckedIn():  boolean { return !!this.todayRecord?.checkIn; }
  get hasCheckedOut(): boolean { return !!this.todayRecord?.checkOut; }
}