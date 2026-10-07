import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  LabService, LabRequestResponse, LabRequestItemResponse,
  STATUS_LABELS, PRIORITY_LABELS
} from '../../../core/services/lab.service';

@Component({
  selector: 'app-lab-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lab-request-detail.component.html',
  styleUrls: ['./lab-request-detail.component.scss']
})
export class LabRequestDetailComponent implements OnInit {

  request: LabRequestResponse | null = null;
  loading = true;
  error = '';
  saving = false;

  statusLabels = STATUS_LABELS;
  priorityLabels = PRIORITY_LABELS;

  // Form state per item
  editingItemId: string | null = null;
  resultForm = {
    resultValue: '',
    resultUnit: '',
    referenceRange: '',
    abnormal: false,
    resultNotes: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private labService: LabService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadRequest(id);
  }

  loadRequest(id: string): void {
    this.loading = true;
    this.labService.findRequestById(id).subscribe({
      next: (req) => { this.request = req; this.loading = false; },
      error: () => { this.error = 'Failed to load request.'; this.loading = false; }
    });
  }

  startEditing(item: LabRequestItemResponse): void {
    this.editingItemId = item.id;
    this.resultForm = {
      resultValue: item.resultValue || '',
      resultUnit: item.resultUnit || '',
      referenceRange: item.referenceRange || '',
      abnormal: item.abnormal || false,
      resultNotes: item.resultNotes || ''
    };
  }

  cancelEditing(): void {
    this.editingItemId = null;
  }

  saveResult(item: LabRequestItemResponse): void {
    if (!this.request) return;
    this.saving = true;

    this.labService.submitResult(this.request.id, item.id, this.resultForm).subscribe({
      next: (updated) => {
        this.request = updated;
        this.editingItemId = null;
        this.saving = false;
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Failed to save result.';
        this.saving = false;
      }
    });
  }

  collect(): void {
    if (!this.request) return;
    this.labService.collect(this.request.id).subscribe({
      next: (updated) => { this.request = updated; }
    });
  }

  startAnalysis(): void {
    if (!this.request) return;
    this.labService.startAnalysis(this.request.id).subscribe({
      next: (updated) => { this.request = updated; }
    });
  }

  cancelRequest(): void {
    if (!this.request) return;
    this.labService.cancel(this.request.id).subscribe({
      next: (updated) => { this.request = updated; },
      error: (err) => { this.error = err.error?.message ?? 'Failed to cancel.'; }
    });
  }

  goBack(): void { this.router.navigate(['/lab']); }
}