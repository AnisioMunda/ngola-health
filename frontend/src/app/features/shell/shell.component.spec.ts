import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  let component: ShellComponent;

  beforeEach(() => {
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);
    component = new ShellComponent(router, authService);
  });

  it('shows only navigation items allowed for the current roles', () => {
    component.currentUser = createUser(['DOCTOR']);

    expect(component.visibleNavItems.map((item) => item.route)).toEqual([
      '/dashboard',
      '/patients',
      '/episodes',
      '/scheduling',
      '/inpatient',
      '/lab',
      '/pharmacy',
      '/prescriptions',
      '/triage',
      '/telemedicine',
    ]);
  });

  it('combines permissions for users with multiple roles', () => {
    component.currentUser = createUser(['PHARMACIST', 'FINANCIAL']);

    expect(component.visibleNavItems.map((item) => item.route)).toEqual([
      '/dashboard',
      '/pharmacy',
      '/financial',
      '/prescriptions',
    ]);
  });

  it('does not expose clinical triage records to managers or receptionists', () => {
    component.currentUser = createUser(['MANAGER', 'RECEPTIONIST']);

    expect(component.visibleNavItems.map((item) => item.route)).not.toContain('/triage');
  });

  it('fails closed when no recognized role is present', () => {
    component.currentUser = createUser(['PATIENT', 'UNKNOWN']);

    expect(component.visibleNavItems).toEqual([]);
  });

  it('shows platform hospital management to super-administrators', () => {
    component.currentUser = createUser(['SUPER_ADMIN']);

    expect(component.visibleNavItems.map((item) => item.route)).toEqual([
      '/dashboard',
      '/hospitals',
    ]);
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
