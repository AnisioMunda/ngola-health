import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  InpatientService,
  CreateBedRequest,
  CreateWardRequest,
  WardResponse,
  BedResponse,
  EditableBedStatus,
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
  private readonly destroyRef = inject(DestroyRef);
  private wardLoadSequence = 0;
  private bedLoadSequence = 0;

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
  savingBedStatusId: string | null = null;

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
      name: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)]],
      code: ['', [Validators.required, Validators.maxLength(20), Validators.pattern(/\S/)]],
      type: ['GENERAL', Validators.required],
      floor: ['', Validators.maxLength(10)],
      notes: ['', Validators.maxLength(500)],
      responsibleDoctorId: [''],
    });

    this.bedForm = this.fb.group({
      bedNumber: ['', [Validators.required, Validators.maxLength(10), Validators.pattern(/\S/)]],
      type: ['STANDARD', Validators.required],
      notes: ['', Validators.maxLength(300)],
    });
  }

  load(preferredWardId?: string): void {
    const requestSequence = ++this.wardLoadSequence;
    this.loading = true;
    this.error = '';
    this.inpatientService
      .findAllWards()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (w) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.wards = w;
          this.loading = false;
          const selectedWard =
            w.find((ward) => ward.id === preferredWardId) ??
            w.find((ward) => ward.id === this.selectedWardId) ??
            w[0];
          const nextWardId = selectedWard?.id ?? '';
          if (nextWardId !== this.selectedWardId) {
            this.selectedWardId = nextWardId;
            this.loadBeds();
          } else if (!nextWardId) {
            this.bedLoadSequence++;
            this.beds = [];
            this.loadingBeds = false;
          }
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.wardLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar enfermarias.');
          this.wards = [];
          this.selectedWardId = '';
          this.bedLoadSequence++;
          this.beds = [];
          this.loadingBeds = false;
          this.loading = false;
        },
      });
  }

  loadBeds(): void {
    const requestSequence = ++this.bedLoadSequence;
    if (!this.selectedWardId) {
      this.beds = [];
      this.loadingBeds = false;
      return;
    }
    const wardId = this.selectedWardId;
    this.loadingBeds = true;
    this.inpatientService
      .findBedsByWard(wardId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (b) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.beds = b;
          this.loadingBeds = false;
        },
        error: (error: HttpErrorResponse) => {
          if (requestSequence !== this.bedLoadSequence) return;
          this.error = this.errorMessage(error, 'Erro ao carregar camas.');
          this.beds = [];
          this.loadingBeds = false;
        },
      });
  }

  loadDoctors(): void {
    this.userService
      .findAll(0, 100)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (p) => {
          this.doctors = p.content
            .filter((u) => u.roles?.includes('DOCTOR'))
            .map((u) => ({ id: u.id, fullName: u.fullName }));
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao carregar a lista de médicos.');
        },
      });
  }

  onWardSelect(): void {
    this.error = '';
    this.loadBeds();
  }

  createWard(): void {
    if (this.savingWard) return;
    if (this.wardForm.invalid) {
      this.wardForm.markAllAsTouched();
      return;
    }
    this.savingWard = true;
    this.error = '';
    this.success = '';
    const value = this.wardForm.getRawValue();
    const request: CreateWardRequest = {
      name: value.name.trim(),
      code: value.code.trim().toUpperCase(),
      type: value.type,
      floor: (value.floor ?? '').trim() || null,
      notes: (value.notes ?? '').trim() || null,
      responsibleDoctorId: value.responsibleDoctorId || null,
    };
    this.inpatientService
      .createWard(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (w) => {
          this.savingWard = false;
          this.showWardForm = false;
          this.wardForm.reset({
            name: '',
            code: '',
            type: 'GENERAL',
            floor: '',
            notes: '',
            responsibleDoctorId: '',
          });
          this.flash('Enfermaria criada com sucesso.');
          this.load(w.id);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao criar enfermaria.');
          this.savingWard = false;
        },
      });
  }

  createBed(): void {
    if (this.savingBed) return;
    if (this.bedForm.invalid || !this.selectedWardId) {
      this.bedForm.markAllAsTouched();
      return;
    }
    this.savingBed = true;
    this.error = '';
    this.success = '';
    const value = this.bedForm.getRawValue();
    const request: CreateBedRequest = {
      wardId: this.selectedWardId,
      bedNumber: value.bedNumber.trim(),
      type: value.type,
      notes: (value.notes ?? '').trim() || null,
    };
    this.inpatientService
      .createBed(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingBed = false;
          this.showBedForm = false;
          this.bedForm.reset({ bedNumber: '', type: 'STANDARD', notes: '' });
          this.flash('Cama criada com sucesso.');
          this.loadBeds();
          this.load(this.selectedWardId);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao criar cama.');
          this.savingBed = false;
        },
      });
  }

  setBedStatus(bedId: string, status: EditableBedStatus): void {
    if (this.savingBedStatusId) return;
    this.savingBedStatusId = bedId;
    this.error = '';
    this.inpatientService
      .updateBedStatus(bedId, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.savingBedStatusId = null;
          this.flash('Estado actualizado.');
          this.loadBeds();
          this.load(this.selectedWardId);
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.errorMessage(error, 'Erro ao actualizar cama.');
          this.savingBedStatusId = null;
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
    const parts = name
      .trim()
      .split(/\s+/)
      .filter((part) => part.length > 0);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  getSelectedWardName(): string {
    const ward = this.wards.find((w) => w.id === this.selectedWardId);
    return ward ? ward.name : '';
  }

  private errorMessage(error: HttpErrorResponse, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? fallback;
  }
}
