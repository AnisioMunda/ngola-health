import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  SchedulingService,
  CalendarEvent,
  STATUS_LABELS,
  AppointmentStatus,
} from '../../../core/services/scheduling.service';
import { UserManagementService } from '../../../core/services/user-management.service';

interface CalendarDay {
  date: Date;
  dateStr: string;
  isToday: boolean;
  isCurrentMonth: boolean;
  events: CalendarEvent[];
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.scss'],
})
export class CalendarComponent implements OnInit {
  currentDate = new Date();
  weeks: CalendarDay[][] = [];
  events: CalendarEvent[] = [];
  loading = false;
  error = '';

  selectedDoctorId = '';
  doctors: { id: string; fullName: string }[] = [];

  statusLabels = STATUS_LABELS;

  readonly DAY_NAMES = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
  readonly MONTH_NAMES = [
    'Janeiro',
    'Fevereiro',
    'Março',
    'Abril',
    'Maio',
    'Junho',
    'Julho',
    'Agosto',
    'Setembro',
    'Outubro',
    'Novembro',
    'Dezembro',
  ];

  constructor(
    private schedulingService: SchedulingService,
    private userService: UserManagementService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadDoctors();
    this.buildCalendar();
    this.loadEvents();
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (p) => {
        this.doctors = p.content
          .filter((u) => u.roles?.includes('DOCTOR'))
          .map((u) => ({ id: u.id, fullName: u.fullName }));
      },
    });
  }

  buildCalendar(): void {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    // Começar no domingo antes do primeiro dia
    const start = new Date(firstDay);
    start.setDate(start.getDate() - start.getDay());

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    this.weeks = [];
    const current = new Date(start);

    while (current <= lastDay || this.weeks.length < 6) {
      const week: CalendarDay[] = [];
      for (let i = 0; i < 7; i++) {
        const d = new Date(current);
        d.setHours(0, 0, 0, 0);
        week.push({
          date: d,
          dateStr: this.toDateStr(d),
          isToday: d.getTime() === today.getTime(),
          isCurrentMonth: d.getMonth() === month,
          events: [],
        });
        current.setDate(current.getDate() + 1);
      }
      this.weeks.push(week);
      if (current > lastDay && this.weeks.length >= 5) break;
    }
  }

  loadEvents(): void {
    this.loading = true;
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    const from = this.toDateStr(new Date(year, month, 1));
    const to = this.toDateStr(new Date(year, month + 1, 0));

    this.schedulingService.getCalendar(from, to).subscribe({
      next: (events) => {
        this.events = events.filter(
          (e) =>
            !this.selectedDoctorId || e.doctorName === this.getDoctorName(this.selectedDoctorId),
        );
        this.distributeEvents();
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar calendário.';
        this.loading = false;
      },
    });
  }

  distributeEvents(): void {
    // Limpar eventos anteriores
    this.weeks.forEach((week) => week.forEach((day) => (day.events = [])));

    this.events.forEach((event) => {
      this.weeks.forEach((week) => {
        const day = week.find((d) => d.dateStr === event.date);
        if (day) day.events.push(event);
      });
    });
  }

  prevMonth(): void {
    this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1);
    this.buildCalendar();
    this.loadEvents();
  }

  nextMonth(): void {
    this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1);
    this.buildCalendar();
    this.loadEvents();
  }

  goToToday(): void {
    this.currentDate = new Date();
    this.buildCalendar();
    this.loadEvents();
  }

  onDoctorFilter(): void {
    this.distributeEvents();
  }

  openEvent(event: CalendarEvent): void {
    this.router.navigate(['/scheduling', event.id]);
  }

  goToCreate(): void {
    this.router.navigate(['/scheduling/new']);
  }
  goToList(): void {
    this.router.navigate(['/scheduling']);
  }

  get monthLabel(): string {
    return `${this.MONTH_NAMES[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
  }

  private toDateStr(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  private getDoctorName(id: string): string {
    return this.doctors.find((d) => d.id === id)?.fullName ?? '';
  }

  statusColor(status: AppointmentStatus): string {
    return (
      {
        SCHEDULED: '#3b82f6',
        CONFIRMED: '#16a34a',
        COMPLETED: '#6b7280',
        CANCELLED: '#ef4444',
        NO_SHOW: '#f59e0b',
      }[status] ?? '#6b7280'
    );
  }
}
