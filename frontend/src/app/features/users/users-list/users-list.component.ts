import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  UserManagementService,
  UserResponse,
} from '../../../core/services/user-management.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.scss'],
})
export class UsersListComponent implements OnInit {
  users: UserResponse[] = [];
  loading = true;
  error = '';

  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  constructor(
    private userService: UserManagementService,
    private router: Router,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.userService.findAll(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.users = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Não foi possível carregar os utilizadores. Tente novamente.';
        this.loading = false;
      },
    });
  }

  get canManageUsers(): boolean {
    return this.authService.getCurrentUser()?.roles?.includes('ADMIN') ?? false;
  }

  goToCreate(): void {
    this.router.navigate(['/users/new']);
  }

  goToEdit(id: string): void {
    this.router.navigate(['/users', id, 'edit']);
  }

  toggleStatus(user: UserResponse): void {
    const action =
      user.registerStatus === 'ACTIVE'
        ? this.userService.deactivate(user.id)
        : this.userService.activate(user.id);

    action.subscribe({
      next: (updated) => {
        const index = this.users.findIndex((u) => u.id === updated.id);
        if (index !== -1) this.users[index] = updated;
      },
      error: () => {
        this.error = 'Não foi possível actualizar o estado do utilizador.';
      },
    });
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Activo',
      INACTIVE: 'Inactivo',
      SUSPENDED: 'Suspenso',
    };
    return map[status] ?? status;
  }

  getRoleLabel(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Administrador',
      DOCTOR: 'Médico',
      NURSE: 'Enfermeiro',
      RECEPTIONIST: 'Recepcionista',
      PHARMACIST: 'Farmacêutico',
      FINANCIAL: 'Financeiro',
      MANAGER: 'Gestor',
      LAB_TECHNICIAN: 'Técnico de laboratório',
      SUPER_ADMIN: 'Superadministrador da plataforma',
    };
    return map[role] ?? role;
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadUsers();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadUsers();
    }
  }
}
