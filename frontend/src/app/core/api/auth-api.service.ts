import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { SKIP_AUTH, SKIP_REFRESH } from '../interceptors/http-context-tokens';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private readonly unauthenticatedContext = new HttpContext().set(SKIP_AUTH, true).set(SKIP_REFRESH, true);

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.config.apiUrl('/api/v1/auth/register'), request, {
      context: this.unauthenticatedContext
    });
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.config.apiUrl('/api/v1/auth/login'), request, {
      context: this.unauthenticatedContext,
      withCredentials: true
    });
  }

  bootstrapCsrf(): Observable<unknown> {
    return this.http.get(this.config.apiUrl('/actuator/health/liveness'), {
      context: this.unauthenticatedContext,
      withCredentials: true
    });
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.config.apiUrl('/api/v1/auth/refresh'), {}, {
      context: this.unauthenticatedContext,
      withCredentials: true
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(this.config.apiUrl('/api/v1/auth/logout'), {}, {
      context: this.unauthenticatedContext,
      withCredentials: true
    });
  }

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(this.config.apiUrl('/api/v1/auth/me'));
  }
}
