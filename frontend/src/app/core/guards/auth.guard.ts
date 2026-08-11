import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../auth/auth-state.service';

export const authGuard: CanActivateFn = () => {
  const state = inject(AuthStateService);
  const router = inject(Router);
  return state.isAuthenticated() ? true : router.createUrlTree(['/login']);
};
