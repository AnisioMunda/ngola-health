import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {
  SchedulingService,
  DoctorScheduleResponse,
} from '../../../core/services/scheduling.service';
import { UserManagementService } from '../../../core/services/user-management.service';

interface DoctorScheduleGroup {
  doctorId: string;
  doctorName: string;
  specialty: string;
  initials: string;
  schedules: DoctorScheduleResponse[];
}

function scheduleTimeRangeValidator(control: AbstractControl): ValidationErrors | null {
  const startTime = control.get('startTime')?.value as string | null;
  const endTime = control.get('endTime')?.value as string | null;
  const duration = Number(control.get('slotDurationMinutes')?.value);
  if (!startTime || !endTime) return null;

  const startMinutes = timeToMinutes(startTime);
  const endMinutes = timeToMinutes(endTime);
  if (endMinutes <= startMinutes) return { invalidTimeRange: true };
  if (duration > endMinutes - startMinutes) return { slotExceedsInterval: true };
  return null;
}

function timeToMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

@Component({
  selector: 'app-doctor-schedules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor-schedules.component.html',
  styleUrls: ['./doctor-schedules.component.scss'],
})
export class DoctorSchedulesComponent implements OnInit {
  schedules: DoctorScheduleResponse[] = [];
  loading = true;
  saving = false;
  error = '';
  success = '';
  doctorError = '';

  showForm = false;
  doctors: { id: string; fullName: string; especiality: string }[] = [];
  form!: FormGroup;

  days = [
    { value: 0, label: 'Segunda-feira' },
    { value: 1, label: 'Terça-feira' },
    { value: 2, label: 'Quarta-feira' },
    { value: 3, label: 'Quinta-feira' },
    { value: 4, label: 'Sexta-feira' },
    { value: 5, label: 'Sábado' },
    { value: 6, label: 'Domingo' },
  ];

  get schedulesByDoctor(): DoctorScheduleGroup[] {
    const map = new Map<string, DoctorScheduleGroup>();
    for (const s of this.schedules) {
      if (!map.has(s.doctorId)) {
        map.set(s.doctorId, {
          doctorId: s.doctorId,
          doctorName: s.doctorName,
          specialty: s.specialty,
          initials: this.getInitials(s.doctorName),
          schedules: [],
        });
      }
      map.get(s.doctorId)?.schedules.push(s);
    }
    return Array.from(map.values());
  }

  constructor(
    private fb: FormBuilder,
    private schedulingService: SchedulingService,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadDoctors();
    this.load();
  }

  buildForm(): void {
    this.form = this.fb.group({
      doctorId: ['', Validators.required],
      dayOfWeek: [0, Validators.required],
      startTime: ['08:00', Validators.required],
      endTime: ['13:00', Validators.required],
      slotDurationMinutes: [30, [Validators.required, Validators.min(10), Validators.max(120)]],
      maxPatientsPerSlot: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
    });
    this.form.addValidators(scheduleTimeRangeValidator);
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.schedulingService.getHospitalSchedules().subscribe({
      next: (s) => {
        this.schedules = s;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar horários.';
        this.loading = false;
      },
    });
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (p) => {
        this.doctors = p.content
          .filter((u) => u.roles?.includes('DOCTOR'))
          .map((u) => ({ id: u.id, fullName: u.fullName, especiality: u.especiality ?? '' }));
        this.doctorError = '';
      },
      error: () => {
        this.doctorError = 'Erro ao carregar a lista de médicos.';
      },
    });
  }

  onSubmit(): void {
    if (this.saving) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    this.success = '';

    this.schedulingService.createSchedule(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form.reset({
          dayOfWeek: 0,
          startTime: '08:00',
          endTime: '13:00',
          slotDurationMinutes: 30,
          maxPatientsPerSlot: 1,
        });
        this.flash('Horário criado com sucesso.');
        this.load();
      },
      error: (err) => {
        this.error = err.error?.detail ?? err.error?.message ?? 'Erro ao criar horário.';
        this.saving = false;
      },
    });
  }

  delete(id: string): void {
    if (!confirm('Remover este horário?')) return;
    this.error = '';
    this.success = '';
    this.schedulingService.deleteSchedule(id).subscribe({
      next: () => {
        this.flash('Horário removido.');
        this.load();
      },
      error: () => {
        this.error = 'Erro ao remover horário.';
      },
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3000);
  }

  // Calcular nº de slots gerados
  slotsPerDay(s: DoctorScheduleResponse): number {
    return Math.floor(
      (this.timeToMinutes(s.endTime) - this.timeToMinutes(s.startTime)) / s.slotDurationMinutes,
    );
  }

  // Preview no formulário
  slotsPreview(): number {
    const start = this.timeToMinutes(this.f['startTime'].value);
    const end = this.timeToMinutes(this.f['endTime'].value);
    const dur = Number(this.f['slotDurationMinutes'].value);
    if (!dur || end <= start) return 0;
    return Math.floor((end - start) / dur);
  }

  hasScheduleOnDay(schedules: DoctorScheduleResponse[], day: number): boolean {
    return schedules.some((s) => s.dayOfWeek === day);
  }

  getSchedulesForDay(schedules: DoctorScheduleResponse[], day: number): DoctorScheduleResponse[] {
    return schedules.filter((s) => s.dayOfWeek === day);
  }

  // Gera iniciais sem arrow functions (seguro para templates)
  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ').filter((p) => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  private timeToMinutes(t: string): number {
    return t ? timeToMinutes(t) : 0;
  }

  get f() {
    return this.form.controls;
  }
}
