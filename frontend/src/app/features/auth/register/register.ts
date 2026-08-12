import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../auth-form.css'
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly site = inject(SiteExperienceService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(12)]]
  });

  constructor() {
    this.site.loadSettings().subscribe();
  }

  submit(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const credentials = this.form.getRawValue();
    this.loading.set(true);
    this.auth.register(credentials).pipe(
      switchMap(() => this.auth.login(credentials)),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: () => void this.router.navigate(['/app']),
      error: () => this.error.set('Registration failed. The account may already exist or the request was invalid.')
    });
  }
}
