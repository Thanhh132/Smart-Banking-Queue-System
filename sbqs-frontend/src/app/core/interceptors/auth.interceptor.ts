import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (request.url.includes('/api/auth/')) {
    return next(request);
  }

  const token = authService.getAccessToken();

  if (!token) {
    return next(request);
  }

  const authorizedRequest = addAuthHeader(request, token);

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || !localStorage.getItem('refreshToken')) {
        return throwError(() => error);
      }

      return authService.refresh().pipe(
        switchMap(() => next(addAuthHeader(request, authService.getAccessToken()))),
        catchError((refreshError) => {
          authService.logout().subscribe();
          router.navigateByUrl('/login');
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function addAuthHeader(request: any, token: string) {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
