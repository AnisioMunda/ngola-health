import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type AppointmentType   = 'OUTPATIENT' | 'EMERGENCY' | 'EXAM' | 'SURGERY' | 'FOLLOW_UP';

export interface DoctorScheduleResponse {
  id: string;
  doctorId: string;
  doctorName: string;
  specialty: string;
  dayOfWeek: number;
  dayLabel: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  maxPatientsPerSlot: number;
  active: boolean;
}

export interface SlotResponse {
  date: string;
  startTime: string;
  endTime: string;
  doctorId: string;
  doctorName: string;
  available: boolean;
  bookedCount: number;
  maxPatients: number;
}

export interface DayAvailabilityResponse {
  date: string;
  dateLabel: string;
  dayOfWeek: string;
  slots: SlotResponse[];
  totalSlots: number;
  availableSlots: number;
}

export interface AppointmentResponse {
  id: string;
  patientId: string;
  patientName: string;
  patientPhone: string;
  doctorId: string;
  doctorName: string;
  doctorSpecialty: string;
  appointmentDate: string;
  appointmentDateLabel: string;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  statusLabel: string;
  appointmentType: AppointmentType;
  reason: string;
  notes: string;
  cancellationReason: string;
  bookedByName: string;
  createdAt: string;
  confirmedAt: string;
}

export interface CalendarEvent {
  id: string;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  patientName: string;
  doctorName: string;
  status: AppointmentStatus;
  color: string;
}

export interface CreateAppointmentRequest {
  patientId: string;
  doctorId: string;
  appointmentDate: string;
  startTime: string;
  appointmentType?: AppointmentType;
  reason: string;
  notes?: string;
}

export interface CreateScheduleRequest {
  doctorId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  maxPatientsPerSlot: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Agendado',
  CONFIRMED: 'Confirmado',
  COMPLETED: 'Realizado',
  CANCELLED: 'Cancelado',
  NO_SHOW:   'Não Compareceu'
};

export const TYPE_LABELS: Record<AppointmentType, string> = {
  OUTPATIENT: 'Ambulatório',
  EMERGENCY:  'Urgência',
  EXAM:       'Exame',
  SURGERY:    'Cirurgia',
  FOLLOW_UP:  'Seguimento'
};

export const DAY_LABELS = [
  'Segunda-feira', 'Terça-feira', 'Quarta-feira',
  'Quinta-feira', 'Sexta-feira', 'Sábado', 'Domingo'
];

@Injectable({ providedIn: 'root' })
export class SchedulingService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/scheduling`;

  // Horários
  getHospitalSchedules(): Observable<DoctorScheduleResponse[]> {
    return this.http.get<DoctorScheduleResponse[]>(`${this.apiUrl}/schedules`);
  }

  getDoctorSchedules(doctorId: string): Observable<DoctorScheduleResponse[]> {
    return this.http.get<DoctorScheduleResponse[]>(
      `${this.apiUrl}/schedules/doctor/${doctorId}`);
  }

  createSchedule(req: CreateScheduleRequest): Observable<DoctorScheduleResponse> {
    return this.http.post<DoctorScheduleResponse>(`${this.apiUrl}/schedules`, req);
  }

  deleteSchedule(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/schedules/${id}`);
  }

  // Disponibilidade
  getAvailability(doctorId: string, date: string): Observable<DayAvailabilityResponse> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('date', date);
    return this.http.get<DayAvailabilityResponse>(`${this.apiUrl}/availability`, { params });
  }

  // Calendário
  getCalendar(from: string, to: string): Observable<CalendarEvent[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<CalendarEvent[]>(`${this.apiUrl}/calendar`, { params });
  }

  // Agendamentos
  findAll(
    doctorId?: string, patientId?: string,
    status?: AppointmentStatus, date?: string,
    page = 0, size = 20
  ): Observable<Page<AppointmentResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (doctorId)  params = params.set('doctorId', doctorId);
    if (patientId) params = params.set('patientId', patientId);
    if (status)    params = params.set('status', status);
    if (date)      params = params.set('date', date);
    return this.http.get<Page<AppointmentResponse>>(
      `${this.apiUrl}/appointments`, { params });
  }

  findById(id: string): Observable<AppointmentResponse> {
    return this.http.get<AppointmentResponse>(`${this.apiUrl}/appointments/${id}`);
  }

  create(req: CreateAppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.apiUrl}/appointments`, req);
  }

  confirm(id: string): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(
      `${this.apiUrl}/appointments/${id}/confirm`, {});
  }

  complete(id: string): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(
      `${this.apiUrl}/appointments/${id}/complete`, {});
  }

  cancel(id: string, reason: string): Observable<AppointmentResponse> {
    const params = new HttpParams().set('reason', reason);
    return this.http.patch<AppointmentResponse>(
      `${this.apiUrl}/appointments/${id}/cancel`, {}, { params });
  }

  noShow(id: string): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(
      `${this.apiUrl}/appointments/${id}/no-show`, {});
  }

  createBlock(req: any): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/blocks`, req);
  }
}