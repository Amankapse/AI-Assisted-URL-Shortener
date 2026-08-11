import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../auth/auth-state.service';

export const adminGuard: CanActivateFn = () => {
  const state = inject(AuthStateService);
  const router = inject(Router);
  return state.isAdmin() ? true : router.createUrlTree(['/app']);
};
