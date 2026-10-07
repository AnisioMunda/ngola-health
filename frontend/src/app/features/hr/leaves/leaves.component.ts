import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  HrService,
  LeaveRequestResponse,
  LEAVE_TYPE_LABELS,
  LEAVE_STATUS_LABELS,
} from '../../../core/services/hr.service';

@Component({
  selector: 'app-leaves',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './leaves.component.html',
  styleUrls: ['./leaves.component.scss'],
})
export class LeavesComponent implements OnInit {
  pendingLeaves: LeaveRequestResponse[] = [];
  myLeaves: LeaveRequestResponse[] = [];

  loading = true;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  activeTab: 'pending' | 'mine' = 'pending';

  totalPages = 0;
  currentPage = 0;

  form!: FormGroup;

  leaveTypeLabels = LEAVE_TYPE_LABELS;
  leaveStatusLabels = LEAVE_STATUS_LABELS;

  leaveTypes = Object.entries(LEAVE_TYPE_LABELS).map(([value, label]) => ({ value, label }));

  constructor(
    private fb: FormBuilder,
    private hrService: HrService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.load();
  }

  buildForm(): void {
    this.form = this.fb.group({
      leaveType: ['VACATION', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      reason: [''],
    });
  }

  load(): void {
    this.loading = true;
    this.hrService.getPendingLeaves().subscribe({
      next: (l) => {
        this.pendingLeaves = l;
        this.loadMine();
      },
      error: () => {
        this.error = 'Erro ao carregar pedidos.';
        this.loading = false;
      },
    });
  }

  loadMine(): void {
    this.hrService.getMyLeaves(this.currentPage).subscribe({
      next: (p) => {
        this.myLeaves = p.content;
        this.totalPages = p.totalPages;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.hrService.requestLeave(this.form.getRawValue()).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form.reset({ leaveType: 'VACATION' });
        this.flash('Pedido de folga submetido.');
        this.load();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao submeter pedido.';
        this.saving = false;
      },
    });
  }

  approve(id: string): void {
    this.hrService.approveLeave(id, true).subscribe({
      next: () => {
        this.flash('Pedido aprovado.');
        this.load();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao aprovar.';
      },
    });
  }

  reject(id: string): void {
    const reason = prompt('Motivo da rejeição:');
    if (!reason) return;
    this.hrService.approveLeave(id, false, reason).subscribe({
      next: () => {
        this.flash('Pedido rejeitado.');
        this.load();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao rejeitar.';
      },
    });
  }

  cancel(id: string): void {
    if (!confirm('Cancelar este pedido?')) return;
    this.hrService.cancelLeave(id).subscribe({
      next: () => {
        this.flash('Pedido cancelado.');
        this.load();
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Erro ao cancelar.';
      },
    });
  }

  flash(msg: string): void {
    this.success = msg;
    setTimeout(() => (this.success = ''), 3000);
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadMine();
    }
  }
  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadMine();
    }
  }

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }
  get f() {
    return this.form.controls;
  }
  goBack(): void {
    this.router.navigate(['/hr']);
  }

  totalDaysPreview(): number {
    const s = this.f['startDate'].value;
    const e = this.f['endDate'].value;
    if (!s || !e) return 0;
    const diff = new Date(e).getTime() - new Date(s).getTime();
    return Math.max(0, Math.floor(diff / 86400000) + 1);
  }
}
