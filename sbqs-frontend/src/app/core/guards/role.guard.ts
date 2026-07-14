import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Chặn route ở phía giao diện và đưa người dùng về trang chủ đúng role.
 * Backend vẫn là lớp quyết định quyền cuối cùng cho mọi API.
 */
export function roleGuard(allowedRoles: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }

    const role = authService.getRole();

    if (!allowedRoles.includes(role)) {
      return router.createUrlTree([authService.getHomeRoute(role)]);
    }

    return true;
  };
}
