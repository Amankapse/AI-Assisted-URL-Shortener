import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ProblemDetails } from './problem-details.model';

@Injectable({ providedIn: 'root' })
export class ProblemDetailsService {
  fromHttpError(error: HttpErrorResponse): ProblemDetails {
    const body = isProblem(error.error) ? error.error : {};
    return {
      type: body.type,
      title: body.title ?? fallbackTitle(error.status),
      status: body.status ?? error.status,
      detail: body.detail ?? error.message,
      instance: body.instance,
      errorCode: body.errorCode,
      correlationId: body.correlationId,
      retryAfterSeconds: parseRetryAfter(error.headers.get('Retry-After'))
    };
  }
}

function isProblem(value: unknown): value is ProblemDetails {
  return !!value && typeof value === 'object';
}

function fallbackTitle(status: number): string {
  if (status === 0) {
    return 'Service unavailable';
  }
  if (status === 429) {
    return 'Too many requests';
  }
  if (status >= 500) {
    return 'Server error';
  }
  return 'Request failed';
}

function parseRetryAfter(value: string | null): number | undefined {
  if (!value) {
    return undefined;
  }
  const seconds = Number(value);
  return Number.isFinite(seconds) && seconds >= 0 ? seconds : undefined;
}
