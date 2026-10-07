import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ShiftType    = 'MORNING' | 'AFTERNOON' | 'NIGHT' | 'FULL_DAY' | 'ON_CALL';
export type ShiftStatus  = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'SWAPPED';
export type LeaveType    = 'VACATION' | 'SICK_LEAVE' | 'PERSONAL' | 'MATERNITY' |
                           'PATERNITY' | 'BEREAVEMENT' | 'UNPAID' | 'COMPENSATORY';
export type LeaveStatus  = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'ON_LEAVE' | 'HOLIDAY';

export interface ShiftResponse {
  id: string;
  userId: string;
  userFullName: string;
  userRole: string;
  shiftType: ShiftType;
  shiftTypeLabel: string;
  shiftDate: string;
  startTime: string;
  endTime: string;
  durationHours: number;
  department: string;
  wardId: string;
  wardName: string;
  status: ShiftStatus;
  statusLabel: string;
  notes: string;
  createdAt: string;
}

export interface DaySchedule {
  date: string;
  dayLabel: string;
  shifts: ShiftResponse[];
  totalShifts: number;
  confirmedShifts: number;
}

export interface WeeklyScheduleResponse {
  weekStart: string;
  weekEnd:   string;
  days: DaySchedule[];
}

export interface LeaveRequestResponse {
  id: string;
  userId: string;
  userFullName: string;
  leaveType: LeaveType;
  leaveTypeLabel: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string;
  status: LeaveStatus;
  statusLabel: string;
  approvedByName: string;
  approvedAt: string;
  rejectionReason: string;
  createdAt: string;
}

export interface AttendanceResponse {
  id: string;
  userId: string;
  userFullName: string;
  workDate: string;
  checkIn: string;
  checkOut: string;
  minutesWorked: number;
  hoursWorked: string;
  overtimeMinutes: number;
  status: AttendanceStatus;
  statusLabel: string;
  notes: string;
  createdAt: string;
}

export interface HrStatsDto {
  totalStaff: number;
  shiftsToday: number;
  pendingLeaves: number;
  presentToday: number;
  absentToday: number;
  onLeaveToday: number;
  todayShifts: ShiftResponse[];
  pendingLeaveRequests: LeaveRequestResponse[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const SHIFT_TYPE_LABELS: Record<ShiftType, string> = {
  MORNING:   'Manhã',
  AFTERNOON: 'Tarde',
  NIGHT:     'Noite',
  FULL_DAY:  'Dia Completo',
  ON_CALL:   'Chamada'
};

export const SHIFT_TYPE_COLORS: Record<ShiftType, string> = {
  MORNING:   '#3b82f6',
  AFTERNOON: '#f59e0b',
  NIGHT:     '#7c3aed',
  FULL_DAY:  '#16a34a',
  ON_CALL:   '#dc2626'
};

export const SHIFT_STATUS_LABELS: Record<ShiftStatus, string> = {
  SCHEDULED: 'Agendado',
  CONFIRMED: 'Confirmado',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
  SWAPPED:   'Trocado'
};

export const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  VACATION:     'Férias',
  SICK_LEAVE:   'Baixa Médica',
  PERSONAL:     'Pessoal',
  MATERNITY:    'Maternidade',
  PATERNITY:    'Paternidade',
  BEREAVEMENT:  'Luto',
  UNPAID:       'Sem Vencimento',
  COMPENSATORY: 'Compensatória'
};

export const LEAVE_STATUS_LABELS: Record<LeaveStatus, string> = {
  PENDING:   'Pendente',
  APPROVED:  'Aprovado',
  REJECTED:  'Rejeitado',
  CANCELLED: 'Cancelado'
};

export const ATTENDANCE_STATUS_LABELS: Record<AttendanceStatus, string> = {
  PRESENT:  'Presente',
  ABSENT:   'Ausente',
  LATE:     'Atrasado',
  ON_LEAVE: 'Em Licença',
  HOLIDAY:  'Feriado'
};

@Injectable({ providedIn: 'root' })
export class HrService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/hr`;

  // Stats
  getStats(): Observable<HrStatsDto> {
    return this.http.get<HrStatsDto>(`${this.apiUrl}/stats`);
  }

  // Turnos
  getWeeklySchedule(weekStart?: string): Observable<WeeklyScheduleResponse> {
    let params = new HttpParams();
    if (weekStart) params = params.set('weekStart', weekStart);
    return this.http.get<WeeklyScheduleResponse>(
      `${this.apiUrl}/shifts/weekly`, { params });
  }

  getMyShifts(from: string, to: string): Observable<ShiftResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ShiftResponse[]>(`${this.apiUrl}/shifts/my`, { params });
  }

  createShift(req: any): Observable<ShiftResponse> {
    return this.http.post<ShiftResponse>(`${this.apiUrl}/shifts`, req);
  }

  updateShiftStatus(id: string, status: ShiftStatus, notes?: string): Observable<ShiftResponse> {
    return this.http.patch<ShiftResponse>(`${this.apiUrl}/shifts/${id}/status`, { status, notes });
  }

  deleteShift(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/shifts/${id}`);
  }

  // Folgas
  getPendingLeaves(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/leaves/pending`);
  }

  getMyLeaves(page = 0): Observable<Page<LeaveRequestResponse>> {
    const params = new HttpParams().set('page', page).set('size', 20);
    return this.http.get<Page<LeaveRequestResponse>>(
      `${this.apiUrl}/leaves/my`, { params });
  }

  getApprovedLeaves(from: string, to: string): Observable<LeaveRequestResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<LeaveRequestResponse[]>(
      `${this.apiUrl}/leaves/approved`, { params });
  }

  requestLeave(req: any): Observable<LeaveRequestResponse> {
    return this.http.post<LeaveRequestResponse>(`${this.apiUrl}/leaves`, req);
  }

  approveLeave(id: string, approved: boolean, rejectionReason?: string): Observable<LeaveRequestResponse> {
    return this.http.patch<LeaveRequestResponse>(
      `${this.apiUrl}/leaves/${id}/approve`, { approved, rejectionReason });
  }

  cancelLeave(id: string): Observable<LeaveRequestResponse> {
    return this.http.patch<LeaveRequestResponse>(
      `${this.apiUrl}/leaves/${id}/cancel`, {});
  }

  // Ponto
  checkIn(shiftId?: string, notes?: string): Observable<AttendanceResponse> {
    return this.http.post<AttendanceResponse>(
      `${this.apiUrl}/attendance/check-in`, { shiftId, notes });
  }

  checkOut(notes?: string): Observable<AttendanceResponse> {
    return this.http.patch<AttendanceResponse>(
      `${this.apiUrl}/attendance/check-out`, { notes });
  }

  getMyAttendance(from: string, to: string): Observable<AttendanceResponse[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<AttendanceResponse[]>(
      `${this.apiUrl}/attendance/my`, { params });
  }

  getDailyAttendance(date?: string): Observable<AttendanceResponse[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<AttendanceResponse[]>(
      `${this.apiUrl}/attendance/daily`, { params });
  }
}