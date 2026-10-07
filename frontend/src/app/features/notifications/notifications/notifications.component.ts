import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  NotificationService,
  NotificationDto,
  TYPE_ICONS,
  PRIORITY_COLORS,
} from '../../../core/services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit {
  notifications: NotificationDto[] = [];
  loading = true;
  error = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;

  typeIcons = TYPE_ICONS;
  priorityColors = PRIORITY_COLORS;

  readonly PRIORITY_LABELS: Record<string, string> = {
    LOW: 'Baixa',
    MEDIUM: 'Média',
    HIGH: 'Alta',
    CRITICAL: 'Crítica',
  };

  readonly TYPE_LABELS: Record<string, string> = {
    LOW_STOCK: 'Stock Baixo',
    EXPIRING_STOCK: 'Lote a Expirar',
    LAB_RESULT: 'Resultado de Exame',
    APPOINTMENT: 'Consulta',
    APPOINTMENT_CANCELLED: 'Consulta Cancelada',
    INVOICE_OVERDUE: 'Factura em Atraso',
    SYSTEM: 'Sistema',
  };

  constructor(
    private notificationService: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.notificationService.getAll(this.currentPage).subscribe({
      next: (page) => {
        this.notifications = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erro ao carregar notificações.';
        this.loading = false;
      },
    });
  }

  markAsRead(n: NotificationDto): void {
    if (n.read) return;
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        n.read = true;
        this.notificationService.unreadCount.update((c) => Math.max(0, c - 1));
      },
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach((n) => (n.read = true));
        this.notificationService.unreadCount.set(0);
      },
    });
  }

  openNotification(n: NotificationDto): void {
    this.markAsRead(n);
    if (n.actionUrl) this.router.navigateByUrl(n.actionUrl);
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

  get unreadCount() {
    return this.notificationService.unreadCount();
  }

  timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(mins / 60);
    const days = Math.floor(hours / 24);
    if (days > 0) return days + ' dia(s) atrás';
    if (hours > 0) return hours + 'h atrás';
    if (mins > 0) return mins + 'min atrás';
    return 'Agora mesmo';
  }
}
