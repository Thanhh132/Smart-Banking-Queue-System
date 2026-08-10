import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const customerProfileGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.getRole() === 'CUSTOMER' && !authService.isProfileComplete()
    ? router.createUrlTree(['/complete-profile'])
    : true;
};
