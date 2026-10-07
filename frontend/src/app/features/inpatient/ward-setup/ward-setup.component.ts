import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  InpatientService,
  WardResponse,
  BedResponse,
  WARD_TYPE_LABELS,
  BED_STATUS_LABELS,
} from '../../../core/services/inpatient.service';
import { UserManagementService } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-ward-setup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ward-setup.component.html',
  styleUrls: ['./ward-setup.component.scss'],
})
export class WardSetupComponent implements OnInit {
  wards: WardResponse[] = [];
  selectedWardId = '';
  beds: BedResponse[] = [];

  loading = true;
  loadingBeds = false;
  error = '';
  success = '';

  showWardForm = false;
  showBedForm = false;
  savingWard = false;
  savingBed = false;

  wardForm!: FormGroup;
  bedForm!: FormGroup;

  doctors: { id: string; fullName: string }[] = [];

  wardTypeLabels = WARD_TYPE_LABELS;
  bedStatusLabels = BED_STATUS_LABELS;

  wardTypes = Object.entries(WARD_TYPE_LABELS).map(([value, label]) => ({ value, label }));

  bedTypes = [
    { value: 'STANDARD', label: 'Standard' },
    { value: 'PRIVATE', label: 'Privada' },
    { value: 'SEMI_PRIVATE', label: 'Semi-Privada' },
    { value: 'ICU', label: 'UCI' },
    { value: 'ISOLATION', label: 'Isolamento' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private inpatientService: InpatientService,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.loadDoctors();
    this.load();
  }

  buildForms(): void {
    this.wardForm = this.fb.group({
      name: ['', Validators.required],
      code: ['', Validators.required],
      type: ['GENERAL', Validators.required],
      floor: [''],
      notes: [''],
      responsibleDoctorId: [''],
    });

    this.bedForm = this.fb.group({
      bedNumber: ['', Validators.required],
      type: ['STANDARD', Validators.required],
      notes: [''],
    });
  }

  load(): void {
    this.loading = true;
    this.inpatientService.findAllWards().subscribe({
      next: (w) => {
        this.wards = w;
        this.loading = false;
        if (w.length > 0 && !this.selectedWardId) {
          this.selectedWardId = w[0].id;
          this.loadBeds();
        }
      },
      error: () => {
        this.error = 'Erro ao carregar enfermarias.';
        this.loading = false;
      },
    });
  }

  loadBeds(): void {
    if (!this.selectedWardId) return;
    this.loadingBeds = true;
    this.inpatientService.findBedsByWard(this.selectedWardId).subscribe({
      next: (b) => {
        this.beds = b;
        this.loadingBeds = false;
      },
      error: () => {
        this.loadingBeds = false;
      },
    });
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

  onWardSelect(): void {
    this.loadBeds();
  }

  createWard(): void {
    if (this.wardForm.invalid) {
      this.wardForm.markAllAsTouched();
      return;
    }
    this.savingWard = true;
    this.inpatientService.createWard(this.wardForm.getRawValue()).subscribe({
      next: (w) => {
        this.savingWard = false;
        this.showWardForm = false;
        this.wardForm.reset({ type: 'GENERAL' });
        this.flash('Enfermaria criada com sucesso.');
        this.load();
        this.selectedWardId = w.id;
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao criar enfermaria.';
        this.savingWard = false;
      },
    });
  }

  createBed(): void {
    if (this.bedForm.invalid || !this.selectedWardId) {
      this.bedForm.markAllAsTouched();
      return;
    }
    this.savingBed = true;
    this.inpatientService
      .createBed({
        wardId: this.selectedWardId,
        ...this.bedForm.getRawValue(),
      })
      .subscribe({
        next: () => {
          this.savingBed = false;
          this.showBedForm = false;
          this.bedForm.reset({ type: 'STANDARD' });
          this.flash('Cama criada com sucesso.');
          this.loadBeds();
          this.load();
        },
        error: (err) => {
          this.error = err.error?.message ?? 'Erro ao criar cama.';
          this.savingBed = false;
        },
      });
  }

  setBedStatus(bedId: string, status: any): void {
    this.inpatientService.updateBedStatus(bedId, status).subscribe({
      next: () => {
        this.flash('Estado actualizado.');
        this.loadBeds();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao actualizar cama.';
      },
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3000);
  }

  goToMap(): void {
    this.router.navigate(['/inpatient']);
  }

  get wf() {
    return this.wardForm.controls;
  }
  get bf() {
    return this.bedForm.controls;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.split(' ').filter((p) => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  getSelectedWardName(): string {
    const ward = this.wards.find((w) => w.id === this.selectedWardId);
    return ward ? ward.name : '';
  }
}
