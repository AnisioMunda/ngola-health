import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PharmacyService, StockBatchResponse } from '../../../core/services/pharmacy.service';

@Component({
  selector: 'app-expiring-stock',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="padding:2rem;max-width:900px;margin:0 auto">
      <header style="display:flex;align-items:center;gap:16px;margin-bottom:1.5rem">
        <button
          (click)="goBack()"
          style="display:flex;align-items:center;gap:6px;padding:7px 14px;background:none;border:1px solid #e5e7eb;border-radius:8px;font-size:0.875rem;color:#6b7280;cursor:pointer"
        >
          <svg width="16" height="16" viewBox="0 0 20 20" fill="none">
            <path
              d="M12 4L6 10l6 6"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
          Back
        </button>
        <h1 style="font-size:1.4rem;font-weight:700;color:#1a1f2e;margin:0;flex:1">
          ⚠ Expiring Stock
        </h1>
        <div style="display:flex;align-items:center;gap:8px;font-size:0.875rem;color:#6b7280">
          <span>Next</span>
          <select
            [(ngModel)]="days"
            (ngModelChange)="load()"
            style="height:36px;padding:0 10px;border:1.5px solid #e5e7eb;border-radius:8px;font-size:0.875rem;background:white;outline:none"
          >
            <option [value]="7">7 days</option>
            <option [value]="30">30 days</option>
            <option [value]="60">60 days</option>
            <option [value]="90">90 days</option>
          </select>
        </div>
      </header>

      <div
        style="display:flex;align-items:center;justify-content:center;gap:12px;padding:4rem;color:#6b7280"
        *ngIf="loading"
      >
        <div
          style="width:28px;height:28px;border:3px solid #e5e7eb;border-top-color:#203a43;border-radius:50%;animation:spin 0.7s linear infinite"
        ></div>
        <span>Loading...</span>
      </div>

      <div
        style="background:white;border-radius:14px;border:1px solid #e5e7eb;overflow:hidden"
        *ngIf="!loading"
      >
        <table style="width:100%;border-collapse:collapse">
          <thead>
            <tr style="background:#f8f9fb;border-bottom:1px solid #e5e7eb">
              <th
                style="padding:12px 16px;font-size:0.75rem;font-weight:600;color:#6b7280;text-transform:uppercase;text-align:left"
              >
                Medication
              </th>
              <th
                style="padding:12px 16px;font-size:0.75rem;font-weight:600;color:#6b7280;text-transform:uppercase;text-align:left"
              >
                Batch
              </th>
              <th
                style="padding:12px 16px;font-size:0.75rem;font-weight:600;color:#6b7280;text-transform:uppercase;text-align:left"
              >
                Expiry
              </th>
              <th
                style="padding:12px 16px;font-size:0.75rem;font-weight:600;color:#6b7280;text-transform:uppercase;text-align:left"
              >
                Available
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              *ngFor="let b of batches"
              (click)="goTo(b.medicationId)"
              [style.background]="isCritical(b) ? '#fff7f7' : 'white'"
              style="border-bottom:1px solid #f1f5f9;cursor:pointer"
            >
              <td style="padding:14px 16px;font-size:0.875rem;font-weight:600;color:#1a1f2e">
                {{ b.medicationName }}
              </td>
              <td style="padding:14px 16px;font-size:0.875rem;color:#9ca3af">
                {{ b.batchNumber }}
              </td>
              <td style="padding:14px 16px">
                <span
                  [style.color]="isCritical(b) ? '#dc2626' : '#d97706'"
                  style="font-weight:600;font-size:0.875rem"
                >
                  {{ b.expiryDate | date: 'dd/MM/yyyy' }}
                  <span *ngIf="isCritical(b)"> ⚠</span>
                </span>
              </td>
              <td style="padding:14px 16px;font-size:0.875rem;font-weight:700;color:#374151">
                {{ b.quantityAvailable }}
              </td>
            </tr>
            <tr *ngIf="batches.length === 0">
              <td
                colspan="4"
                style="text-align:center;padding:3rem;color:#16a34a;font-weight:600;font-size:0.95rem"
              >
                ✓ No batches expiring in the next {{ days }} days.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class ExpiringStockComponent implements OnInit {
  batches: StockBatchResponse[] = [];
  loading = true;
  days = 30;

  constructor(
    private pharmacyService: PharmacyService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.pharmacyService.findExpiringSoon(this.days).subscribe({
      next: (b) => {
        this.batches = b;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  isCritical(b: StockBatchResponse): boolean {
    const days = Math.ceil((new Date(b.expiryDate).getTime() - Date.now()) / 86400000);
    return days <= 7;
  }

  goTo(medicationId: string): void {
    this.router.navigate(['/pharmacy', medicationId]);
  }
  goBack(): void {
    this.router.navigate(['/pharmacy']);
  }
}
