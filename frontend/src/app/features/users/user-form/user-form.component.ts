import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserManagementService, ROLES } from '../../../core/services/user-management.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss'],
})
export class UserFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;
  userId: string | null = null;
  loading = false;
  saving = false;
  error = '';
  roles = ROLES;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserManagementService,
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.userId;
    this.buildForm();

    if (this.isEdit) {
      this.loadUser();
    }
  }

  buildForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3)]],
      username: [
        { value: '', disabled: this.isEdit },
        [Validators.required, Validators.minLength(3), Validators.pattern('^[a-z0-9._-]+$')],
      ],
      email: ['', [Validators.required, Validators.email]],
      password: [
        this.isEdit ? '' : '',
        this.isEdit ? [] : [Validators.required, Validators.minLength(8)],
      ],
      phone: [''],
      especiality: [''],
      professionalCard: [''],
      mustChangePassword: [true],
      roleIds: [[], [Validators.required]],
    });
  }

  loadUser(): void {
    this.loading = true;
    this.userService.findById(this.userId!).subscribe({
      next: (user) => {
        const roleIds = ROLES.filter((r) => user.roles.includes(r.name)).map((r) => r.id);

        this.form.patchValue({
          fullName: user.fullName,
          username: user.username,
          email: user.email,
          phone: user.phone,
          especiality: user.especiality,
          professionalCard: user.professionalCard,
          mustChangePassword: user.mustChangePassword,
          roleIds,
        });
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load user.';
        this.loading = false;
      },
    });
  }

  isRoleSelected(roleId: string): boolean {
    return (this.form.get('roleIds')?.value ?? []).includes(roleId);
  }

  toggleRole(roleId: string): void {
    const current: string[] = this.form.get('roleIds')?.value ?? [];
    const updated = current.includes(roleId)
      ? current.filter((id) => id !== roleId)
      : [...current, roleId];
    this.form.get('roleIds')?.setValue(updated);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();

    const action = this.isEdit
      ? this.userService.update(this.userId!, {
          fullName: value.fullName,
          email: value.email,
          phone: value.phone,
          especiality: value.especiality,
          professionalCard: value.professionalCard,
          roleIds: value.roleIds,
        })
      : this.userService.create(value);

    action.subscribe({
      next: () => {
        this.router.navigate(['/users']);
      },
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'Failed to save user.';
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/users']);
  }

  get f() {
    return this.form.controls;
  }
}
