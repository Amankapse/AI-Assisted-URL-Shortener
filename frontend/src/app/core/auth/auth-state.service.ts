import { computed, Injectable, signal } from '@angular/core';
import { UserResponse } from '../api/api-types';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly accessTokenSignal = signal<string | null>(null);
  private readonly userSignal = signal<UserResponse | null>(null);
  private readonly initializingSignal = signal(true);

  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly initializing = this.initializingSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.accessTokenSignal() && !!this.userSignal());
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');

  setSession(accessToken: string, user: UserResponse): void {
    this.accessTokenSignal.set(accessToken);
    this.userSignal.set(user);
  }

  setUser(user: UserResponse): void {
    this.userSignal.set(user);
  }

  clear(): void {
    this.accessTokenSignal.set(null);
    this.userSignal.set(null);
  }

  finishInitialization(): void {
    this.initializingSignal.set(false);
  }
}
