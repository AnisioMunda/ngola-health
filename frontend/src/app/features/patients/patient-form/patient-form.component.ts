import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PatientService,
  ANGOLA_PROVINCES,
  BLOOD_TYPES
} from '../../../core/services/patient.service';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.scss']
})
export class PatientFormComponent implements OnInit {

  form!: FormGroup;
  isEdit = false;
  patientId: string | null = null;
  loading = false;
  saving = false;
  error = '';

  provinces = ANGOLA_PROVINCES;
  bloodTypes = BLOOD_TYPES;
  activeTab: 'personal' | 'contact' | 'clinical' = 'personal';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.patientId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.patientId;
    this.buildForm();
    if (this.isEdit) this.loadPatient();
  }

  buildForm(): void {
    this.form = this.fb.group({
      // Personal
      fullName:         ['', [Validators.required, Validators.minLength(3)]],
      birthDate:        ['', Validators.required],
      gender:           ['', Validators.required],
      nationalId:       [''],
      healthCardNumber: [''],
      // Contact
      phone:       [''],
      email:       ['', Validators.email],
      address:     [''],
      province:    [''],
      municipality:[''],
      // Emergency
      emergencyContactName:        [''],
      emergencyContactPhone:       [''],
      emergencyContactRelationship:[''],
      // Clinical
      bloodType:         [''],
      allergies:         [''],
      chronicConditions: [''],
      notes:             ['']
    });
  }

  loadPatient(): void {
    this.loading = true;
    this.patientService.findById(this.patientId!).subscribe({
      next: (p) => {
        this.form.patchValue({
          ...p,
          birthDate: p.birthDate?.substring(0, 10)
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load patient.';
        this.loading = false;
      }
    });
  }

  setTab(tab: 'personal' | 'contact' | 'clinical'): void {
    this.activeTab = tab;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.activeTab = 'personal';
      return;
    }

    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();

    const action = this.isEdit
      ? this.patientService.update(this.patientId!, value)
      : this.patientService.create(value);

    action.subscribe({
      next: () => this.router.navigate(['/patients']),
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'Failed to save patient.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/patients']);
  }

  get f() { return this.form.controls; }
}