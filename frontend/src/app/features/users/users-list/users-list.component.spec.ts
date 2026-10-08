import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../../core/services/auth.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { UsersListComponent } from './users-list.component';

describe('UsersListComponent', () => {
  let component: UsersListComponent;
  let getCurrentUser: jasmine.Spy;

  beforeEach(() => {
    const userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', [
      'findAll',
      'activate',
      'deactivate',
    ]);
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    getCurrentUser = authService.getCurrentUser;

    component = new UsersListComponent(userService, router, authService);
  });

  it('enables user-management actions for hospital administrators', () => {
    getCurrentUser.and.returnValue(createUser(['ADMIN']));

    expect(component.canManageUsers).toBe(true);
  });

  it('keeps user-management actions hidden for managers and unknown roles', () => {
    getCurrentUser.and.returnValue(createUser(['MANAGER']));
    expect(component.canManageUsers).toBe(false);

    getCurrentUser.and.returnValue(createUser(['SUPER_ADMIN']));
    expect(component.canManageUsers).toBe(false);
  });

  it('fails closed when no authenticated user is available', () => {
    getCurrentUser.and.returnValue(null);

    expect(component.canManageUsers).toBe(false);
  });
});

function createUser(roles: string[]): AuthUser {
  return {
    id: 'user-id',
    fullName: 'Utilizador',
    username: 'utilizador',
    email: 'utilizador@example.invalid',
    roles,
    mustChangePassword: false,
  };
}
