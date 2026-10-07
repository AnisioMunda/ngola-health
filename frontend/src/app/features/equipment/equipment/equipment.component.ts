import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import {
  EquipmentService,
  EquipmentResponse,
  EquipmentStatsDto,
  EquipmentStatus,
  MaintenanceType,
  STATUS_COLORS,
  CATEGORY_LABELS,
} from '../../../core/services/equipment.service';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './equipment.component.html',
  styleUrls: ['./equipment.component.scss'],
})
export class EquipmentComponent implements OnInit {
  equipment: EquipmentResponse[] = [];
  stats: EquipmentStatsDto | null = null;
  selected: EquipmentResponse | null = null;
  maintenanceDue: EquipmentResponse[] = [];

  loading = true;
  saving = false;
  error = '';
  success = '';

  searchQuery = '';
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;

  activeTab: 'list' | 'alerts' | 'detail' = 'list';

  showCreateForm = false;
  showMaintenanceForm = false;

  statusColors = STATUS_COLORS;
  categoryLabels = CATEGORY_LABELS;

  statuses: EquipmentStatus[] = ['ACTIVE', 'MAINTENANCE', 'REPAIR', 'RETIRED', 'RESERVED'];
  categories = Object.entries(CATEGORY_LABELS).map(([v, l]) => ({ value: v, label: l }));
  maintenanceTypes: { value: MaintenanceType; label: string }[] = [
    { value: 'PREVENTIVE', label: 'Preventiva' },
    { value: 'CORRECTIVE', label: 'Correctiva' },
    { value: 'CALIBRATION', label: 'Calibração' },
    { value: 'INSPECTION', label: 'Inspecção' },
  ];
  statusList = [
    { value: 'ACTIVE', label: 'Activo' },
    { value: 'MAINTENANCE', label: 'Em Manutenção' },
    { value: 'REPAIR', label: 'Em Reparação' },
    { value: 'RETIRED', label: 'Abatido' },
    { value: 'RESERVED', label: 'Reservado' },
  ];

  createForm!: FormGroup;
  maintenanceForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private equipmentService: EquipmentService,
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.load();
    this.loadStats();
  }

  buildForms(): void {
    this.createForm = this.fb.group({
      name: ['', Validators.required],
      code: ['', Validators.required],
      brand: [''],
      model: [''],
      serialNumber: [''],
      category: ['OTHER'],
      location: [''],
      purchaseDate: [''],
      purchasePrice: [''],
      warrantyExpiry: [''],
      nextMaintenanceDate: [''],
      maintenanceIntervalDays: [365],
      nextCalibrationDate: [''],
      notes: [''],
    });

    this.maintenanceForm = this.fb.group({
      type: ['PREVENTIVE', Validators.required],
      description: ['', Validators.required],
      cost: [''],
      nextMaintenanceDate: [''],
      partsReplaced: [''],
      result: ['OK'],
    });
  }

  load(): void {
    this.loading = true;
    this.equipmentService.findAll(this.searchQuery, this.currentPage).subscribe({
      next: (p) => {
        this.equipment = p.content;
        this.totalPages = p.totalPages;
        this.totalElements = p.totalElements;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar equipamentos.';
        this.loading = false;
      },
    });
  }

  loadStats(): void {
    this.equipmentService.getStats().subscribe({ next: (s) => (this.stats = s) });
  }

  loadAlerts(): void {
    this.equipmentService.findMaintenanceDue().subscribe({
      next: (e) => {
        this.maintenanceDue = e;
      },
    });
  }

  onTabChange(tab: 'list' | 'alerts' | 'detail'): void {
    this.activeTab = tab;
    if (tab === 'alerts') this.loadAlerts();
  }

  onSearch(): void {
    this.currentPage = 0;
    this.load();
  }
  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  openDetail(e: EquipmentResponse): void {
    this.equipmentService.findById(e.id).subscribe({
      next: (full) => {
        this.selected = full;
        this.activeTab = 'detail';
      },
    });
  }

  closeDetail(): void {
    this.selected = null;
    this.activeTab = 'list';
  }

  onCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    const v = this.createForm.getRawValue();
    this.equipmentService
      .create({
        ...v,
        purchasePrice: v.purchasePrice || null,
        purchaseDate: v.purchaseDate || null,
        warrantyExpiry: v.warrantyExpiry || null,
        nextMaintenanceDate: v.nextMaintenanceDate || null,
        nextCalibrationDate: v.nextCalibrationDate || null,
        maintenanceIntervalDays: v.maintenanceIntervalDays || 365,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.showCreateForm = false;
          this.createForm.reset({ category: 'OTHER', maintenanceIntervalDays: 365 });
          this.flash('Equipamento registado com sucesso.');
          this.load();
          this.loadStats();
        },
        error: (e) => {
          this.error = e.error?.message ?? 'Erro.';
          this.saving = false;
        },
      });
  }

  onAddMaintenance(): void {
    if (!this.selected || this.maintenanceForm.invalid) {
      this.maintenanceForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.maintenanceForm.getRawValue();
    this.equipmentService
      .addMaintenance(this.selected.id, {
        ...v,
        cost: v.cost || null,
        nextMaintenanceDate: v.nextMaintenanceDate || null,
        partsReplaced: v.partsReplaced || null,
      })
      .subscribe({
        next: (updated) => {
          this.selected = updated;
          this.showMaintenanceForm = false;
          this.maintenanceForm.reset({ type: 'PREVENTIVE', result: 'OK' });
          this.saving = false;
          this.flash('Manutenção registada.');
          this.load();
          this.loadStats();
        },
        error: (e) => {
          this.error = e.error?.message ?? 'Erro.';
          this.saving = false;
        },
      });
  }

  updateStatus(e: EquipmentResponse, status: EquipmentStatus): void {
    this.equipmentService.updateStatus(e.id, status).subscribe({
      next: (updated) => {
        this.updateInList(updated);
        if (this.selected?.id === updated.id) this.selected = updated;
        this.flash('Estado actualizado.');
        this.loadStats();
      },
    });
  }

  updateInList(updated: EquipmentResponse): void {
    const idx = this.equipment.findIndex((e) => e.id === updated.id);
    if (idx !== -1) this.equipment[idx] = updated;
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3500);
  }

  daysUntil(date: string): number {
    return Math.ceil((new Date(date).getTime() - Date.now()) / 86400000);
  }

  get f() {
    return this.createForm.controls;
  }
  get mf() {
    return this.maintenanceForm.controls;
  }
}
