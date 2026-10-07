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
    expect(password.invalid).toBeTrue();

    password.setValue('password');
    expect(password.invalid).toBeTrue();

    password.setValue('Password1');
    expect(password.valid).toBeTrue();
  });

  it('requires at least one role before submitting', () => {
    const roleIds = component.form.controls['roleIds'];

    roleIds.setValue([]);
    expect(roleIds.invalid).toBeTrue();

    roleIds.setValue(['00000000-0000-0000-0000-000000000001']);
    expect(roleIds.valid).toBeTrue();
  });
});
