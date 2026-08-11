import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthApi } from '../api/auth-api.service';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '../api/api-types';
import { WorkspaceStateService } from '../workspace/workspace-state.service';
import { AuthStateService } from './auth-state.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApi);
  private readonly state = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly workspaceState = inject(WorkspaceStateService);
  private refreshInFlight?: Observable<AuthResponse>;

  initialize(): Observable<AuthResponse | null> {
    return this.api.bootstrapCsrf().pipe(
      switchMap(() => this.refresh()),
      switchMap((auth) => this.api.me().pipe(tap((user) => this.state.setUser(user)), switchMap(() => of(auth)))),
      catchError(() => {
        this.state.clear();
        return of(null);
      }),
      finalize(() => this.state.finishInitialization())
    );
  }

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.api.register(request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.login(request).pipe(
      tap((response) => this.state.setSession(response.accessToken, response.user)),
      tap(() => this.workspaceState.load())
    );
  }

  refresh(): Observable<AuthResponse> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.api.refresh().pipe(
        tap((response) => this.state.setSession(response.accessToken, response.user)),
        finalize(() => {
          this.refreshInFlight = undefined;
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.refreshInFlight;
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(
      catchError(() => of(void 0)),
      tap(() => {
        this.state.clear();
        this.workspaceState.clear();
        void this.router.navigate(['/login']);
      })
    );
  }

  forceLogout(): Observable<never> {
    this.state.clear();
    this.workspaceState.clear();
    void this.router.navigate(['/login']);
    return throwError(() => new Error('Authentication refresh failed.'));
  }
}
