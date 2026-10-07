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
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NotificationBellComponent],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss']
})
export class ShellComponent implements OnInit {

  collapsed    = false;
  currentRoute = '';
  currentUser: any = null;

  navItems: NavItem[] = [
    { label: 'Dashboard',    route: '/dashboard',     icon: 'dashboard'    },
    { label: 'Pacientes',    route: '/patients',      icon: 'patients'     },
    { label: 'Consultas',    route: '/episodes',      icon: 'episodes'     },
    { label: 'Agendamento',  route: '/scheduling',    icon: 'scheduling'   },
    { label: 'Internamentos',route: '/inpatient',  icon: 'inpatient'  },
    { label: 'Laboratório',  route: '/lab',           icon: 'lab'          },
    { label: 'Farmácia',     route: '/pharmacy',      icon: 'pharmacy'     },
    { label: 'Facturação',   route: '/financial',     icon: 'financial'    },
    { label: 'Relatórios',   route: '/reports',       icon: 'reports'      },
    { label: 'Utilizadores', route: '/users',         icon: 'users'        },
    { label: 'Auditoria',    route: '/audit',      icon: 'audit'      },
    { label: 'RH',           route: '/hr',         icon: 'hr'         },
    { label: 'Prescrições',   route: '/prescriptions',  icon: 'prescriptions'  },
    { label: 'Triagem',       route: '/triage',        icon: 'triage'        },
    { label: 'Equipamentos', route: '/equipment',    icon: 'equipment'    },
    { label: 'Telemedicina', route: '/telemedicine', icon: 'telemedicine' }
  ];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentRoute = this.router.url;
    this.currentUser  = this.authService.getCurrentUser?.() ?? null;

    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      this.currentRoute = e.urlAfterRedirects;
    });

    if (window.innerWidth < 768) this.collapsed = true;
  }

  toggle(): void { this.collapsed = !this.collapsed; }

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
    const parts = name.split(' ').filter(p => p.length > 0);
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
}