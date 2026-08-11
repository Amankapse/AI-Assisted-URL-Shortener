import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { AuthStateService } from '../auth/auth-state.service';
import { SKIP_AUTH, SKIP_REFRESH } from './http-context-tokens';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authState = inject(AuthStateService);
  const authService = inject(AuthService);
  const token = authState.accessToken();
  const usedJwt = !!token && !request.context.get(SKIP_AUTH) && !request.headers.has('X-API-Key');
  const csrfToken = csrfCookie();

  let securedRequest = request;
  if (usedJwt) {
    securedRequest = securedRequest.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  if (csrfToken && requiresCsrf(securedRequest.url)) {
    securedRequest = securedRequest.clone({ setHeaders: { 'X-XSRF-TOKEN': csrfToken }, withCredentials: true });
  }

  return next(securedRequest).pipe(
    catchError((error: unknown) => {
      if (shouldRefresh(error, usedJwt, securedRequest.context.get(SKIP_REFRESH))) {
        return authService.refresh().pipe(
          switchMap((response) =>
            next(securedRequest.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } }))
          ),
          catchError(() => authService.forceLogout())
        );
      }
      return throwError(() => error);
    })
  );
};

function shouldRefresh(error: unknown, usedJwt: boolean, skipRefresh: boolean): boolean {
  return usedJwt && !skipRefresh && error instanceof HttpErrorResponse && error.status === 401;
}

function requiresCsrf(url: string): boolean {
  return url.includes('/api/v1/auth/refresh') || url.includes('/api/v1/auth/logout');
}

function csrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}
