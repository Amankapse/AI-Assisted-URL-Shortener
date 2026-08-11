import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../auth/auth-state.service';

export const guestGuard: CanActivateFn = () => {
  const state = inject(AuthStateService);
  const router = inject(Router);
  return state.isAuthenticated() ? router.createUrlTree(['/app']) : true;
};
