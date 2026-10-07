import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  InpatientService, WardResponse, BedResponse
} from '../../../core/services/inpatient.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-admission-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admission-form.component.html',
  styleUrls: ['./admission-form.component.scss']
})
export class AdmissionFormComponent implements OnInit {

  form!: FormGroup;
  saving = false;
  error  = '';

  patients: { id: string; fullName: string; phone: string }[] = [];
  doctors:  { id: string; fullName: string }[]                = [];
  wards:    WardResponse[]                                    = [];
  beds:     BedResponse[]                                     = [];

  loadingBeds = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private inpatientService: InpatientService,
    private patientService: PatientService,
    private userService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadPatients();
    this.loadDoctors();
    this.loadWards();

    // Pré-seleccionar cama se veio do mapa
    const bedId = this.route.snapshot.queryParamMap.get('bedId');
    if (bedId) this.form.patchValue({ bedId });
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId:            ['', Validators.required],
      wardId:               ['', Validators.required],
      bedId:                ['', Validators.required],
      responsibleDoctorId:  ['', Validators.required],
      admissionReason:      ['', [Validators.required, Validators.minLength(10)]],
      expectedDischargeDate:['']
    });

    this.form.get('wardId')?.valueChanges.subscribe(wardId => {
      if (wardId) this.loadBeds(wardId);
      else { this.beds = []; this.form.get('bedId')?.setValue(''); }
    });
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
          .map(u => ({ id: u.id, fullName: u.fullName }));
      }
    });
  }

  loadWards(): void {
    this.inpatientService.findAllWards().subscribe({
      next: (w) => { this.wards = w; }
    });
  }

  loadBeds(wardId: string): void {
    this.loadingBeds = true;
    this.beds = [];
    this.form.get('bedId')?.setValue('');
    this.inpatientService.findBedsByWard(wardId).subscribe({
      next: (beds) => {
        // Mostrar apenas camas disponíveis ou reservadas
        this.beds = beds.filter(b =>
          b.status === 'AVAILABLE' || b.status === 'RESERVED');
        this.loadingBeds = false;
      },
      error: () => { this.loadingBeds = false; }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error  = '';

    const value = this.form.getRawValue();
    this.inpatientService.admit({
      patientId:            value.patientId,
      bedId:                value.bedId,
      responsibleDoctorId:  value.responsibleDoctorId,
      admissionReason:      value.admissionReason,
      expectedDischargeDate:value.expectedDischargeDate || null
    }).subscribe({
      next: (a) => this.router.navigate(['/inpatient/admissions', a.id]),
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar internamento.';
        this.saving = false;
      }
    });
  }

  get minDate(): string { return new Date().toISOString().split('T')[0]; }
  get f() { return this.form.controls; }
  goBack(): void { this.router.navigate(['/inpatient/admissions']); }
}