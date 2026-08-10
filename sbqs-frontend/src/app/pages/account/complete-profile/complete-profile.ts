import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AccountService } from '../../../core/services/account.service';
import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './complete-profile.html',
  styleUrl: './complete-profile.scss',
})
export class CompleteProfile {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  readonly email = sessionStorage.getItem('email') || '';
  isSubmitting = false;
  errorMessage = '';

  profileForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    phone: ['', [Validators.required, Validators.pattern(/^(?:\+84|0)[0-9]{9,10}$/)]],
    permanentAddress: ['', [Validators.required, Validators.maxLength(500)]],
    contactAddress: ['', [Validators.required, Validators.maxLength(500)]],
  });

  constructor() {
    if (this.authService.isProfileComplete()) {
      this.router.navigateByUrl('/customer');
    }
  }

  submit(): void {
    this.errorMessage = '';
    if (this.profileForm.invalid || this.isSubmitting) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const value = this.profileForm.getRawValue();
    this.accountService.completeSocialProfile({
      fullName: value.fullName || '',
      phone: value.phone || '',
      permanentAddress: value.permanentAddress || '',
      contactAddress: value.contactAddress || '',
    }).pipe(finalize(() => {
      this.isSubmitting = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: (profile) => {
        this.authService.markProfileComplete(profile.fullName);
        this.router.navigateByUrl('/customer');
      },
      error: (error) => {
        this.errorMessage = this.apiError.getMessage(
          error,
          'Không thể lưu hồ sơ. Vui lòng kiểm tra lại thông tin.',
        );
      },
    });
  }

  usePermanentAddress(): void {
    this.profileForm.controls.contactAddress.setValue(
      this.profileForm.controls.permanentAddress.value || '',
    );
  }

  logout(): void {
    this.authService.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }
}
