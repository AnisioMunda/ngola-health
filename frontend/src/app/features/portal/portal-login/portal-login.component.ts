// ============================================================
// portal-login.component.ts
// ============================================================
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PortalService } from '../../../core/services/portal.service';

@Component({
  selector: 'app-portal-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './portal-login.component.html',
  styleUrls: ['./portal-login.component.scss']
})
export class PortalLoginComponent {
  form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  loading = false;
  error   = '';
  tab: 'login' | 'register' = 'login';

  registerForm = this.fb.group({
    patientNumber: ['', Validators.required],
    email:         ['', [Validators.required, Validators.email]],
    password:      ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private portalService: PortalService,
    private router: Router
  ) {}

  onLogin(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true; this.error = '';
    const v = this.form.getRawValue();
    this.portalService.login(v.email!, v.password!).subscribe({
      next: () => this.router.navigate(['/portal']),
      error: (e) => { this.error = e.error?.message ?? 'Email ou password incorrectos.'; this.loading = false; }
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) { this.registerForm.markAllAsTouched(); return; }
    const v = this.registerForm.getRawValue();
    if (v.password !== v.confirmPassword) { this.error = 'As passwords não coincidem.'; return; }
    this.loading = true; this.error = '';
    this.portalService.register({
      patientNumber: v.patientNumber,
      email:         v.email,
      password:      v.password
    }).subscribe({
      next: () => this.router.navigate(['/portal']),
      error: (e) => { this.error = e.error?.message ?? 'Erro ao criar conta.'; this.loading = false; }
    });
  }

  get f()  { return this.form.controls; }
  get rf() { return this.registerForm.controls; }
}