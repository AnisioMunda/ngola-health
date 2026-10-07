import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import {
  LabService,
  LabRequestResponse,
  LabRequestItemResponse,
  SubmitResultRequest,
  STATUS_LABELS,
  PRIORITY_LABELS,
  labErrorMessage,
} from '../../../core/services/lab.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-lab-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lab-request-detail.component.html',
  styleUrls: ['./lab-request-detail.component.scss'],
})
export class LabRequestDetailComponent implements OnInit {
  request: LabRequestResponse | null = null;
  loading = true;
  error = '';
  saving = false;
  requestId: string | null = null;

  statusLabels = STATUS_LABELS;
  priorityLabels = PRIORITY_LABELS;

  // Form state per item
  editingItemId: string | null = null;
  resultForm = {
    resultValue: '',
    resultUnit: '',
    referenceRange: '',
    abnormal: false,
    resultNotes: '',
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private labService: LabService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.canViewDetail) {
      this.error = 'O seu perfil não tem permissão para consultar pedidos de laboratório.';
      this.loading = false;
      return;
    }
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadRequest(id);
    } else {
      this.error = 'Não foi indicado o identificador do pedido.';
      this.loading = false;
    }
  }

  get canViewDetail(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return ['ADMIN', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN', 'MANAGER'].some((role) =>
      roles.includes(role),
    );
  }

  get canCollect(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      this.request?.status === 'PENDING' &&
      ['ADMIN', 'NURSE', 'LAB_TECHNICIAN'].some((role) => roles.includes(role))
    );
  }

  get canStartAnalysis(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      this.request?.status === 'COLLECTED' &&
      (roles.includes('ADMIN') || roles.includes('LAB_TECHNICIAN'))
    );
  }

  get canSubmitResults(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      this.request?.status === 'IN_ANALYSIS' &&
      (roles.includes('ADMIN') || roles.includes('LAB_TECHNICIAN'))
    );
  }

  get canCancel(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return (
      ['PENDING', 'COLLECTED', 'IN_ANALYSIS'].includes(this.request?.status ?? '') &&
      ['ADMIN', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN'].some((role) => roles.includes(role))
    );
  }

  loadRequest(id: string): void {
    this.requestId = id;
    this.loading = true;
    this.error = '';
    this.labService.findRequestById(id).subscribe({
      next: (req) => {
        this.request = req;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.request = null;
        this.error = labErrorMessage(error, 'Não foi possível carregar o pedido de laboratório.');
        this.loading = false;
      },
    });
  }

  startEditing(item: LabRequestItemResponse): void {
    if (!this.canSubmitResults || item.resultValue !== null) return;
    this.editingItemId = item.id;
    this.resultForm = {
      resultValue: '',
      resultUnit: '',
      referenceRange: '',
      abnormal: false,
      resultNotes: '',
    };
  }

  cancelEditing(): void {
    this.editingItemId = null;
  }

  saveResult(item: LabRequestItemResponse): void {
    if (!this.request || !this.canSubmitResults || item.resultValue !== null || this.saving) return;
    const resultValue = this.resultForm.resultValue.trim();
    if (!resultValue) {
      this.error = 'O resultado do exame é obrigatório.';
      return;
    }
    if (this.resultForm.resultUnit.length > 30 || this.resultForm.referenceRange.length > 100) {
      this.error = 'A unidade pode ter até 30 caracteres e o intervalo até 100.';
      return;
    }

    const result: SubmitResultRequest = {
      resultValue,
      resultUnit: this.resultForm.resultUnit.trim() || undefined,
      referenceRange: this.resultForm.referenceRange.trim() || undefined,
      abnormal: this.resultForm.abnormal,
      resultNotes: this.resultForm.resultNotes.trim() || undefined,
    };
    this.runRequestAction(
      this.labService.submitResult(this.request.id, item.id, result),
      'Não foi possível registar o resultado.',
      () => {
        this.editingItemId = null;
      },
    );
  }

  collect(): void {
    if (!this.request || !this.canCollect) return;
    this.runRequestAction(
      this.labService.collect(this.request.id),
      'Não foi possível registar a recolha da amostra.',
    );
  }

  startAnalysis(): void {
    if (!this.request || !this.canStartAnalysis) return;
    this.runRequestAction(
      this.labService.startAnalysis(this.request.id),
      'Não foi possível iniciar a análise.',
    );
  }

  cancelRequest(): void {
    if (!this.request || !this.canCancel) return;
    this.runRequestAction(
      this.labService.cancel(this.request.id),
      'Não foi possível cancelar o pedido.',
    );
  }

  private runRequestAction(
    action: Observable<LabRequestResponse>,
    fallback: string,
    onSuccess?: () => void,
  ): void {
    if (this.saving) return;
    this.saving = true;
    this.error = '';
    action.subscribe({
      next: (updated) => {
        this.request = updated;
        onSuccess?.();
        this.saving = false;
      },
      error: (error: unknown) => {
        this.error = labErrorMessage(error, fallback);
        this.saving = false;
      },
    });
  }

  goBack(): void {
    void this.router.navigate(['/lab']);
  }
}
