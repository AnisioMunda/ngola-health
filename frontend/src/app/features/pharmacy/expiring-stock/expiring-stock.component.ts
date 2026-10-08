import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import {
  PharmacyService,
  StockBatchResponse,
  pharmacyErrorMessage,
} from '../../../core/services/pharmacy.service';

function todayInLuanda(): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Africa/Luanda',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

@Component({
  selector: 'app-expiring-stock',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './expiring-stock.component.html',
  styleUrls: ['./expiring-stock.component.scss'],
})
export class ExpiringStockComponent implements OnInit {
  batches: StockBatchResponse[] = [];
  loading = true;
  error = '';
  days = 30;
  readonly expiryWindows = [7, 30, 60, 90];

  constructor(
    private pharmacyService: PharmacyService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.canViewExpiryAlerts) {
      void this.router.navigate(['/pharmacy']);
      return;
    }
    this.load();
  }

  get canViewExpiryAlerts(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    return roles.some((role) => ['ADMIN', 'MANAGER', 'PHARMACIST'].includes(role));
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.pharmacyService.findExpiringSoon(this.days).subscribe({
      next: (batches) => {
        this.batches = batches;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.batches = [];
        this.error = pharmacyErrorMessage(
          error,
          'Não foi possível carregar os alertas de validade.',
        );
        this.loading = false;
      },
    });
  }

  isCritical(batch: StockBatchResponse): boolean {
    const expiry = this.dateAsUtcDay(batch.expiryDate);
    const today = this.dateAsUtcDay(todayInLuanda());
    const daysRemaining = (expiry - today) / 86_400_000;
    return daysRemaining >= 0 && daysRemaining <= 7;
  }

  goTo(medicationId: string): void {
    void this.router.navigate(['/pharmacy', medicationId]);
  }

  goBack(): void {
    void this.router.navigate(['/pharmacy']);
  }

  private dateAsUtcDay(value: string): number {
    const [year, month, day] = value.split('-').map(Number);
    return Date.UTC(year, month - 1, day);
  }
}
