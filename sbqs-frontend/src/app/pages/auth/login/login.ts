import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  isSubmitting = false;
  errorMessage = '';

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const payload = {
      email: this.loginForm.value.email || '',
      password: this.loginForm.value.password || '',
    };

    this.isSubmitting = true;

    this.authService.login(payload).pipe(
      timeout(15000),
      finalize(() => {
        this.isSubmitting = false;
      })
    ).subscribe({
      next: (response) => {
        console.log('Login role:', response.role, 'branchId:', response.branchId);
        this.router.navigateByUrl(this.authService.getHomeRoute(response.role));
      },
      error: (err) => {
        this.errorMessage =
          err?.name === 'TimeoutError'
            ? 'Keycloak phan hoi qua lau. Kiem tra Keycloak va cau hinh client.'
            : err?.error?.message || err?.error || 'Dang nhap that bai.';
      },
    });
  }
}
