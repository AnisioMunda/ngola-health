import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  HospitalManagementService,
  HospitalRequest,
  HospitalType,
} from '../../../core/services/hospital-management.service';

@Component({
  selector: 'app-hospital-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './hospital-form.component.html',
  styleUrls: ['./hospital-form.component.scss'],
})
export class HospitalFormComponent implements OnInit {
  readonly hospitalTypes: { value: HospitalType; label: string }[] = [
    { value: 'HOSPITAL', label: 'Hospital' },
    { value: 'CLINIC', label: 'Clínica' },
    { value: 'POLYCLINIC', label: 'Policlínica' },
    { value: 'HEALTH_CENTER', label: 'Centro de saúde' },
    { value: 'LABORATORY', label: 'Laboratório' },
  ];
  readonly form: FormGroup;
  hospitalId: string | null = null;
  loading = false;
  saving = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private hospitalService: HospitalManagementService,
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', [Validators.required, Validators.pattern('.*\\S.*'), Validators.maxLength(200)]],
      code: ['', [Validators.required, Validators.pattern('^[A-Z0-9-]{3,20}$')]],
      type: this.fb.nonNullable.control<HospitalType | ''>('', Validators.required),
      province: [
        '',
        [Validators.required, Validators.pattern('.*\\S.*'), Validators.maxLength(100)],
      ],
      municipality: ['', Validators.maxLength(100)],
      address: ['', Validators.maxLength(500)],
      phone: ['', Validators.maxLength(20)],
      email: ['', [Validators.email, Validators.maxLength(200)]],
      taxId: ['', Validators.maxLength(50)],
    });
  }

  ngOnInit(): void {
    this.hospitalId = this.route.snapshot.paramMap.get('id');
    if (this.hospitalId) this.loadHospital();
  }

  get isEdit(): boolean {
    return this.hospitalId !== null;
  }

  loadHospital(): void {
    if (!this.hospitalId) return;

    this.loading = true;
    this.hospitalService.findById(this.hospitalId).subscribe({
      next: (hospital) => {
        this.form.patchValue({
          name: hospital.name,
          code: hospital.code,
          type: hospital.type,
          province: hospital.province,
          municipality: hospital.municipality ?? '',
          address: hospital.address ?? '',
          phone: hospital.phone ?? '',
          email: hospital.email ?? '',
          taxId: hospital.taxId ?? '',
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os dados do hospital.';
        this.loading = false;
      },
    });
  }

  normalizeCode(): void {
    const control = this.form.controls['code'];
    const code = control.value.toUpperCase();
    if (code !== control.value) control.setValue(code);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const selectedType = this.hospitalTypes.find((item) => item.value === value.type);
    if (!selectedType) {
      this.form.controls['type'].setErrors({ required: true });
      return;
    }
    this.saving = true;
    this.error = '';
    const request: HospitalRequest = {
      name: value.name.trim(),
      code: value.code.trim().toUpperCase(),
      type: selectedType.value,
      province: value.province.trim(),
      municipality: this.optionalText(value.municipality),
      address: this.optionalText(value.address),
      phone: this.optionalText(value.phone),
      email: this.optionalText(value.email),
      taxId: this.optionalText(value.taxId),
    };
    const action = this.hospitalId
      ? this.hospitalService.update(this.hospitalId, request)
      : this.hospitalService.create(request);

    action.subscribe({
      next: () => {
        void this.router.navigate(['/hospitals']);
      },
      error: (response: { error?: { message?: string } }) => {
        this.saving = false;
        this.error = response.error?.message ?? 'Não foi possível guardar o hospital.';
      },
    });
  }

  goBack(): void {
    void this.router.navigate(['/hospitals']);
  }

  private optionalText(value: string): string | undefined {
    const normalized = value.trim();
    return normalized || undefined;
  }
}
