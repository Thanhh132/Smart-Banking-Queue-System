import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, timeout } from 'rxjs';

import { ApiErrorService } from '../../../core/services/api-error.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-google-callback',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './google-callback.html',
  styleUrl: './google-callback.scss',
})
export class GoogleCallback implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private apiError = inject(ApiErrorService);
  private cdr = inject(ChangeDetectorRef);

  isLoading = true;
  errorMessage = '';

  ngOnInit(): void {
    const providerError = this.route.snapshot.queryParamMap.get('error');
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    if (window.opener && window.opener !== window) {
      window.opener.postMessage(
        {
          type: 'SBQS_GOOGLE_OAUTH_CALLBACK',
          code,
          state,
          error: providerError,
        },
        window.location.origin,
      );
      window.close();
      return;
    }

    const expectedState = sessionStorage.getItem('googleOAuthState');
    const verifier = sessionStorage.getItem('googlePkceVerifier');
    sessionStorage.removeItem('googleOAuthState');
    sessionStorage.removeItem('googlePkceVerifier');
    this.authService.clearLocalSession();

    if (providerError) {
      this.fail('Bạn đã hủy hoặc Google từ chối yêu cầu đăng nhập.');
      return;
    }
    if (!code || !state || !expectedState || state !== expectedState || !verifier) {
      this.fail('Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn.');
      return;
    }

    this.authService.exchangeGoogleCode(code, verifier).pipe(
      timeout(20000),
      finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }),
    ).subscribe({
      next: (response) => this.router.navigateByUrl(this.authService.getPostLoginRoute(response)),
      error: (error) => {
        this.authService.clearLocalSession();
        this.errorMessage = this.apiError.getMessage(
          error,
          'Không thể hoàn tất đăng nhập Google. Vui lòng thử lại.',
        );
      },
    });
  }

  private fail(message: string): void {
    this.authService.clearLocalSession();
    this.isLoading = false;
    this.errorMessage = message;
    this.cdr.detectChanges();
  }
}
