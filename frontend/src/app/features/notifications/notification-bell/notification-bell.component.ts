import { Component, OnInit, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  NotificationService,
  NotificationDto,
  TYPE_ICONS,
  PRIORITY_COLORS,
  notificationErrorMessage,
} from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss'],
})
export class NotificationBellComponent implements OnInit {
  open = false;
  notifications: NotificationDto[] = [];
  loading = false;
  error = '';

  typeIcons = TYPE_ICONS;
  priorityColors = PRIORITY_COLORS;

  notificationService = inject(NotificationService);
  private router = inject(Router);

  get unreadCount() {
    return this.notificationService.unreadCount();
  }

  ngOnInit(): void {
    this.notificationService.startPolling();
    this.loadTopUnread();
  }

  toggleOpen(): void {
    this.open = !this.open;
    if (this.open) this.loadTopUnread();
  }

  loadTopUnread(): void {
    this.loading = true;
    this.error = '';
    this.notificationService.getTopUnread().subscribe({
      next: (n) => {
        this.notifications = n;
        this.loading = false;
      },
      error: (error: unknown) => {
        this.error = notificationErrorMessage(error, 'Não foi possível carregar as notificações.');
        this.loading = false;
      },
    });
  }

  markAsRead(n: NotificationDto, event: Event): void {
    event.stopPropagation();
    this.markNotificationAsRead(n);
  }

  private markNotificationAsRead(n: NotificationDto): void {
    if (n.read) return;
    this.error = '';
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        n.read = true;
        this.notifications = this.notifications.filter((notification) => notification.id !== n.id);
        this.notificationService.unreadCount.update((c) => Math.max(0, c - 1));
      },
      error: (error: unknown) => {
        this.error = notificationErrorMessage(
          error,
          'Não foi possível marcar a notificação como lida.',
        );
      },
    });
  }

  markAllAsRead(): void {
    this.error = '';
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications = [];
        this.notificationService.unreadCount.set(0);
      },
      error: (error: unknown) => {
        this.error = notificationErrorMessage(
          error,
          'Não foi possível marcar todas as notificações como lidas.',
        );
      },
    });
  }

  open_(n: NotificationDto): void {
    this.markNotificationAsRead(n);
    if (n.actionUrl) {
      this.router.navigateByUrl(n.actionUrl);
    }
    this.open = false;
  }

  goToAll(): void {
    this.router.navigate(['/notifications']);
    this.open = false;
  }

  // Fechar ao clicar fora
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-bell')) {
      this.open = false;
    }
  }

  timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(mins / 60);
    const days = Math.floor(hours / 24);
    if (days > 0) return days + 'd atrás';
    if (hours > 0) return hours + 'h atrás';
    if (mins > 0) return mins + 'min atrás';
    return 'Agora';
  }
}
