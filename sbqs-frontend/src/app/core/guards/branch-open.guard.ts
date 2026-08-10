import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { BranchService } from '../services/branch.service';

export const branchOpenGuard: CanActivateFn = () => {
  const branchService = inject(BranchService);
  const router = inject(Router);
  const branchId = Number(sessionStorage.getItem('selectedBranchId'));

  if (!Number.isFinite(branchId) || branchId <= 0) {
    return router.createUrlTree(['/branches']);
  }

  return branchService.getOpenStatus(branchId).pipe(
    map((status) =>
      status.openNow
        ? true
        : router.createUrlTree(['/branches'], { queryParams: { closedBranch: branchId } }),
    ),
    catchError(() =>
      of(router.createUrlTree(['/branches'], { queryParams: { scheduleUnavailable: true } })),
    ),
  );
};
