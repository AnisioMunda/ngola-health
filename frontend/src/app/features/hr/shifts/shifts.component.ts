import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  HrService, WeeklyScheduleResponse, DaySchedule, ShiftResponse,
  SHIFT_TYPE_LABELS, SHIFT_TYPE_COLORS, SHIFT_STATUS_LABELS
} from '../../../core/services/hr.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { InpatientService, WardResponse } from '../../../core/services/inpatient.service';

@Component({
  selector: 'app-shifts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './shifts.component.html',
  styleUrls: ['./shifts.component.scss']
})
export class ShiftsComponent implements OnInit {

  schedule: WeeklyScheduleResponse | null = null;
  loading  = true;
  saving   = false;
  error    = '';
  success  = '';

  showForm    = false;
  currentWeek = new Date();

  form!: FormGroup;

  staff:  { id: string; fullName: string }[] = [];
  wards:  WardResponse[] = [];

  shiftTypeLabels  = SHIFT_TYPE_LABELS;
  shiftTypeColors  = SHIFT_TYPE_COLORS;
  shiftStatusLabels = SHIFT_STATUS_LABELS;

  shiftTypes = Object.entries(SHIFT_TYPE_LABELS).map(([value, label]) => ({ value, label }));

  constructor(
    private fb: FormBuilder,
    private hrService: HrService,
    private userService: UserManagementService,
    private inpatientService: InpatientService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadStaff();
    this.loadWards();
    this.loadSchedule();
  }

  buildForm(): void {
    this.form = this.fb.group({
      userId:    ['', Validators.required],
      shiftType: ['MORNING', Validators.required],
      shiftDate: ['', Validators.required],
      startTime: ['08:00', Validators.required],
      endTime:   ['16:00', Validators.required],
      wardId:    [''],
      department:[''],
      notes:     ['']
    });

    // Auto preencher horas por tipo de turno
    this.form.get('shiftType')?.valueChanges.subscribe(type => {
      const times: Record<string, [string, string]> = {
        MORNING:   ['07:00', '15:00'],
        AFTERNOON: ['15:00', '23:00'],
        NIGHT:     ['23:00', '07:00'],
        FULL_DAY:  ['08:00', '17:00'],
        ON_CALL:   ['08:00', '08:00']
      };
      if (times[type]) {
        this.form.patchValue({ startTime: times[type][0], endTime: times[type][1] });
      }
    });
  }

  loadStaff(): void {
    this.userService.findAll(0, 200).subscribe({
      next: (p) => {
        this.staff = p.content.map(u => ({ id: u.id, fullName: u.fullName }));
      }
    });
  }

  loadWards(): void {
    this.inpatientService.findAllWards().subscribe({
      next: (w) => { this.wards = w; }
    });
  }

  loadSchedule(): void {
    this.loading = true;
    const weekStart = this.getWeekStart(this.currentWeek);
    this.hrService.getWeeklySchedule(weekStart).subscribe({
      next: (s) => { this.schedule = s; this.loading = false; },
      error: () => { this.error = 'Erro ao carregar escala.'; this.loading = false; }
    });
  }

  prevWeek(): void {
    this.currentWeek = new Date(this.currentWeek.setDate(this.currentWeek.getDate() - 7));
    this.loadSchedule();
  }

  nextWeek(): void {
    this.currentWeek = new Date(this.currentWeek.setDate(this.currentWeek.getDate() + 7));
    this.loadSchedule();
  }

  thisWeek(): void { this.currentWeek = new Date(); this.loadSchedule(); }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error  = '';

    const v = this.form.getRawValue();
    this.hrService.createShift({
      userId:    v.userId,
      shiftType: v.shiftType,
      shiftDate: v.shiftDate,
      startTime: v.startTime,
      endTime:   v.endTime,
      wardId:    v.wardId || null,
      department:v.department || null,
      notes:     v.notes || null
    }).subscribe({
      next: () => {
        this.saving   = false;
        this.showForm = false;
        this.form.reset({ shiftType: 'MORNING', startTime: '07:00', endTime: '15:00' });
        this.flash('Turno criado com sucesso.');
        this.loadSchedule();
      },
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar turno.';
        this.saving = false;
      }
    });
  }

  confirmShift(id: string): void {
    this.hrService.updateShiftStatus(id, 'CONFIRMED').subscribe({
      next: () => { this.flash('Turno confirmado.'); this.loadSchedule(); }
    });
  }

  deleteShift(id: string): void {
    if (!confirm('Eliminar este turno?')) return;
    this.hrService.deleteShift(id).subscribe({
      next: () => { this.flash('Turno eliminado.'); this.loadSchedule(); }
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  getWeekStart(date: Date): string {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    return d.toISOString().split('T')[0];
  }

  get weekLabel(): string {
    if (!this.schedule) return '';
    return this.formatDate(this.schedule.weekStart)
      + ' — ' + this.formatDate(this.schedule.weekEnd);
  }

  formatDate(d: string): string {
    return new Date(d + 'T00:00:00').toLocaleDateString('pt-PT',
      { day: '2-digit', month: 'short' });
  }

  isToday(dateStr: string): boolean {
    return dateStr === new Date().toISOString().split('T')[0];
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ').filter(p => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  get f() { return this.form.controls; }

  goBack(): void { this.router.navigate(['/hr']); }
}