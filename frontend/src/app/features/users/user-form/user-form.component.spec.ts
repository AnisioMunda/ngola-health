import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { UserManagementService } from '../../../core/services/user-management.service';
import { UserFormComponent } from './user-form.component';

describe('UserFormComponent', () => {
  let component: UserFormComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UserFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
        { provide: UserManagementService, useValue: {} },
      ],
    });

    component = TestBed.createComponent(UserFormComponent).componentInstance;
    component.buildForm();
  });

  it('requires an initial password matching the backend policy', () => {
    const password = component.form.controls['password'];

    password.setValue('');
    expect(password.invalid).toBe(true);

    password.setValue('password');
    expect(password.invalid).toBe(true);

    password.setValue('Password1');
    expect(password.valid).toBe(true);
  });

  it('requires at least one role before submitting', () => {
    const roleIds = component.form.controls['roleIds'];

    roleIds.setValue([]);
    expect(roleIds.invalid).toBe(true);

    roleIds.setValue(['00000000-0000-0000-0000-000000000001']);
    expect(roleIds.valid).toBe(true);
  });

  it('requires a valid Entra object ID when the doctor role is selected', () => {
    component.form.controls['roleIds'].setValue(['00000000-0000-0000-0000-000000000002']);
    expect(component.isDoctorRoleSelected()).toBe(true);

    const teamsUserId = component.form.controls['teamsUserId'];
    teamsUserId.setValue('invalid');
    expect(teamsUserId.invalid).toBe(true);

    teamsUserId.setValue('c7ad8e3d-a0a6-4d41-b44f-06c31bc662c4');
    expect(teamsUserId.valid).toBe(true);
  });
});
