import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../error/notification.service';
import { ProblemDetailsService } from '../error/problem-details.service';
import { SKIP_REFRESH } from './http-context-tokens';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const problems = inject(ProblemDetailsService);
  const notifications = inject(NotificationService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !request.context.get(SKIP_REFRESH)) {
        notifications.showError(problems.fromHttpError(error));
      }
      return throwError(() => error);
    })
  );
};
