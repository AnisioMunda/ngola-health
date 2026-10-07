import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  SchedulingService, DayAvailabilityResponse,
  SlotResponse, TYPE_LABELS, DAY_LABELS
} from '../../../core/services/scheduling.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.scss']
})
export class AppointmentFormComponent implements OnInit {

  form!: FormGroup;
  saving = false;
  error  = '';

  patients: { id: string; fullName: string; phone: string }[] = [];
  doctors:  { id: string; fullName: string; specialty: string }[] = [];

  availability: DayAvailabilityResponse | null = null;
  loadingSlots = false;
  selectedSlot: SlotResponse | null = null;

  typeLabels = TYPE_LABELS;
  dayLabels  = DAY_LABELS;

  appointmentTypes = [
    { value: 'OUTPATIENT', label: 'Consulta Ambulatório' },
    { value: 'EMERGENCY',  label: 'Urgência'             },
    { value: 'EXAM',       label: 'Exame'                },
    { value: 'SURGERY',    label: 'Cirurgia'             },
    { value: 'FOLLOW_UP',  label: 'Seguimento'           }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private schedulingService: SchedulingService,
    private patientService: PatientService,
    private userService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadPatients();
    this.loadDoctors();
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId:       ['', Validators.required],
      doctorId:        ['', Validators.required],
      appointmentDate: ['', Validators.required],
      startTime:       ['', Validators.required],
      appointmentType: ['OUTPATIENT', Validators.required],
      reason:          ['', [Validators.required, Validators.minLength(5)]],
      notes:           ['']
    });

    // Quando médico ou data mudar, buscar slots
    this.form.get('doctorId')?.valueChanges.subscribe(() => this.onDoctorOrDateChange());
    this.form.get('appointmentDate')?.valueChanges.subscribe(() => this.onDoctorOrDateChange());
  }

  onDoctorOrDateChange(): void {
    const doctorId = this.form.get('doctorId')?.value;
    const date     = this.form.get('appointmentDate')?.value;
    if (doctorId && date) {
      this.loadSlots(doctorId, date);
    } else {
      this.availability = null;
      this.selectedSlot = null;
    }
    this.form.get('startTime')?.setValue('');
    this.selectedSlot = null;
  }

  loadSlots(doctorId: string, date: string): void {
    this.loadingSlots = true;
    this.availability = null;
    this.schedulingService.getAvailability(doctorId, date).subscribe({
      next: (av) => { this.availability = av; this.loadingSlots = false; },
      error: () => { this.loadingSlots = false; }
    });
  }

  selectSlot(slot: SlotResponse): void {
    if (!slot.available) return;
    this.selectedSlot = slot;
    this.form.get('startTime')?.setValue(slot.startTime);
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => {
        this.patients = p.content.map(x => ({
          id: x.id, fullName: x.fullName, phone: x.phone ?? ''
        }));
      }
    });
  }

  loadDoctors(): void {
    this.userService.findAll(0, 100).subscribe({
      next: (p) => {
        this.doctors = p.content
          .filter(u => u.roles?.includes('DOCTOR'))
          .map(u => ({ id: u.id, fullName: u.fullName, specialty: u.especiality ?? '' }));
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error  = '';

    this.schedulingService.create(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/scheduling']),
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar agendamento.';
        this.saving = false;
      }
    });
  }

  get minDate(): string { return new Date().toISOString().split('T')[0]; }
  get f() { return this.form.controls; }
  goBack(): void { this.router.navigate(['/scheduling']); }
}