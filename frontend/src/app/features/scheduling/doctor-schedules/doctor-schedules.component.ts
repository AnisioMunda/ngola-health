import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import {
  SchedulingService, DoctorScheduleResponse
} from '../../../core/services/scheduling.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-doctor-schedules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './doctor-schedules.component.html',
  styleUrls: ['./doctor-schedules.component.scss']
})
export class DoctorSchedulesComponent implements OnInit {

  schedules: DoctorScheduleResponse[] = [];
  loading  = true;
  saving   = false;
  error    = '';
  success  = '';

  showForm = false;
  doctors: { id: string; fullName: string; especiality: string }[] = [];
  form!: FormGroup;

  days = [
    { value: 0, label: 'Segunda-feira' },
    { value: 1, label: 'Terça-feira'   },
    { value: 2, label: 'Quarta-feira'  },
    { value: 3, label: 'Quinta-feira'  },
    { value: 4, label: 'Sexta-feira'   },
    { value: 5, label: 'Sábado'        },
    { value: 6, label: 'Domingo'       }
  ];

  get schedulesByDoctor(): {
    doctorId: string; doctorName: string;
    specialty: string; initials: string;
    schedules: DoctorScheduleResponse[]
  }[] {
    const map = new Map<string, any>();
    for (const s of this.schedules) {
      if (!map.has(s.doctorId)) {
        map.set(s.doctorId, {
          doctorId: s.doctorId,
          doctorName: s.doctorName,
          specialty: s.specialty,
          initials: this.getInitials(s.doctorName),
          schedules: []
        });
      }
      map.get(s.doctorId).schedules.push(s);
    }
    return Array.from(map.values());
  }

  constructor(
    private fb: FormBuilder,
    private schedulingService: SchedulingService,
    private userService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadDoctors();
    this.load();
  }

  buildForm(): void {
    this.form = this.fb.group({
      doctorId:            ['', Validators.required],
      dayOfWeek:           [0,  Validators.required],
      startTime:           ['08:00', Validators.required],
      endTime:             ['13:00', Validators.required],
      slotDurationMinutes: [30,  [Validators.required, Validators.min(10), Validators.max(120)]],
      maxPatientsPerSlot:  [1,   [Validators.required, Validators.min(1),  Validators.max(10)]]
    });
  }

  load(): void {
    this.loading = true;
    this.schedulingService.getHospitalSchedules().subscribe({
      next: (s) => { this.schedules = s; this.loading = false; },
      error: () => { this.error = 'Erro ao carregar horários.'; this.loading = false; }
    });
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (p) => {
        this.doctors = p.content
          .filter(u => u.roles?.includes('DOCTOR'))
          .map(u => ({ id: u.id, fullName: u.fullName, especiality: u.especiality ?? '' }));
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error  = '';

    this.schedulingService.createSchedule(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving   = false;
        this.showForm = false;
        this.form.reset({
          dayOfWeek: 0, startTime: '08:00', endTime: '13:00',
          slotDurationMinutes: 30, maxPatientsPerSlot: 1
        });
        this.flash('Horário criado com sucesso.');
        this.load();
      },
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar horário.';
        this.saving = false;
      }
    });
  }

  delete(id: string): void {
    if (!confirm('Remover este horário?')) return;
    this.schedulingService.deleteSchedule(id).subscribe({
      next: () => { this.flash('Horário removido.'); this.load(); },
      error: () => { this.error = 'Erro ao remover horário.'; }
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 3000);
  }

  // Calcular nº de slots gerados
  slotsPerDay(s: DoctorScheduleResponse): number {
    return Math.floor(
      (this.timeToMinutes(s.endTime) - this.timeToMinutes(s.startTime))
      / s.slotDurationMinutes
    );
  }

  // Preview no formulário
  slotsPreview(): number {
    const start = this.timeToMinutes(this.f['startTime'].value);
    const end   = this.timeToMinutes(this.f['endTime'].value);
    const dur   = Number(this.f['slotDurationMinutes'].value);
    if (!dur || end <= start) return 0;
    return Math.floor((end - start) / dur);
  }

  hasScheduleOnDay(schedules: DoctorScheduleResponse[], day: number): boolean {
    return schedules.some(s => s.dayOfWeek === day);
  }

  getSchedulesForDay(schedules: DoctorScheduleResponse[], day: number): DoctorScheduleResponse[] {
    return schedules.filter(s => s.dayOfWeek === day);
  }

  // Gera iniciais sem arrow functions (seguro para templates)
  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ').filter(p => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  private timeToMinutes(t: string): number {
    if (!t) return 0;
    const parts = t.split(':');
    return Number(parts[0]) * 60 + Number(parts[1]);
  }

  get f() { return this.form.controls; }
}