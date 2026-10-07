import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { NotificationBellComponent } from '../notifications/notification-bell/notification-bell.component';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles: readonly string[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NotificationBellComponent],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent implements OnInit {
  collapsed = false;
  currentRoute = '';
  currentUser: ReturnType<AuthService['getCurrentUser']> = null;

  navItems: NavItem[] = [
    {
      label: 'Dashboard',
      route: '/dashboard',
      icon: 'dashboard',
      roles: [
        'ADMIN',
        'MANAGER',
        'DOCTOR',
        'NURSE',
        'RECEPTIONIST',
        'PHARMACIST',
        'FINANCIAL',
        'LAB_TECHNICIAN',
        'SUPER_ADMIN',
      ],
    },
    {
      label: 'Hospitais',
      route: '/hospitals',
      icon: 'hospitals',
      roles: ['SUPER_ADMIN'],
    },
    {
      label: 'Pacientes',
      route: '/patients',
      icon: 'patients',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'NURSE', 'RECEPTIONIST'],
    },
    {
      label: 'Consultas',
      route: '/episodes',
      icon: 'episodes',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'NURSE', 'RECEPTIONIST'],
    },
    {
      label: 'Agendamento',
      route: '/scheduling',
      icon: 'scheduling',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'NURSE', 'RECEPTIONIST'],
    },
    {
      label: 'Internamentos',
      route: '/inpatient',
      icon: 'inpatient',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'NURSE'],
    },
    {
      label: 'Laboratório',
      route: '/lab',
      icon: 'lab',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN'],
    },
    {
      label: 'Farmácia',
      route: '/pharmacy',
      icon: 'pharmacy',
      roles: ['ADMIN', 'MANAGER', 'PHARMACIST', 'NURSE', 'DOCTOR'],
    },
    {
      label: 'Facturação',
      route: '/financial',
      icon: 'financial',
      roles: ['ADMIN', 'MANAGER', 'FINANCIAL'],
    },
    {
      label: 'Relatórios',
      route: '/reports',
      icon: 'reports',
      roles: ['ADMIN', 'MANAGER'],
    },
    {
      label: 'Utilizadores',
      route: '/users',
      icon: 'users',
      roles: ['ADMIN', 'MANAGER'],
    },
    {
      label: 'Auditoria',
      route: '/audit',
      icon: 'audit',
      roles: ['ADMIN', 'MANAGER'],
    },
    { label: 'RH', route: '/hr', icon: 'hr', roles: ['ADMIN', 'MANAGER'] },
    {
      label: 'Prescrições',
      route: '/prescriptions',
      icon: 'prescriptions',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR', 'PHARMACIST', 'NURSE'],
    },
    {
      label: 'Triagem',
      route: '/triage',
      icon: 'triage',
      roles: ['ADMIN', 'DOCTOR', 'NURSE'],
    },
    {
      label: 'Equipamentos',
      route: '/equipment',
      icon: 'equipment',
      roles: ['ADMIN', 'MANAGER'],
    },
    {
      label: 'Telemedicina',
      route: '/telemedicine',
      icon: 'telemedicine',
      roles: ['ADMIN', 'MANAGER', 'DOCTOR'],
    },
  ];

  constructor(
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.currentRoute = this.router.url;
    this.currentUser = this.authService.getCurrentUser();

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.currentRoute = event.urlAfterRedirects;
      });

    if (window.innerWidth < 768) this.collapsed = true;
  }

  get visibleNavItems(): NavItem[] {
    const roles = this.currentUser?.roles ?? [];
    return this.navItems.filter((item) => item.roles.some((role) => roles.includes(role)));
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  goTo(route: string): void {
    this.router.navigate([route]);
    if (window.innerWidth < 768) this.collapsed = true;
  }

  isActive(route: string): boolean {
    return this.currentRoute.startsWith(route);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    const parts = name.split(' ').filter((p) => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
}
