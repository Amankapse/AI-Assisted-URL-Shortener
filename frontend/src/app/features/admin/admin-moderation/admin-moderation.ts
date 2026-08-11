import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApi } from '../../../core/api/admin-api.service';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { NotificationService } from '../../../core/error/notification.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ErrorState } from '../../../shared/components/error-state/error-state';

@Component({
  selector: 'app-admin-moderation',
  imports: [ReactiveFormsModule, ConfirmDialog, ErrorState],
  templateUrl: './admin-moderation.html',
  styleUrl: './admin-moderation.css'
})
export class AdminModeration {
  private readonly admin = inject(AdminApi);
  private readonly fb = inject(FormBuilder);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);

  readonly problem = signal<ProblemDetails | null>(null);
  readonly action = signal<'block' | 'unblock' | null>(null);
  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    urlId: ['', [Validators.required, Validators.pattern(/^[0-9a-fA-F-]{36}$/)]]
  });

  request(action: 'block' | 'unblock'): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.action.set(action);
  }

  confirm(): void {
    const action = this.action();
    const id = this.form.getRawValue().urlId.trim();
    if (!action) {
      return;
    }
    this.saving.set(true);
    const call = action === 'block' ? this.admin.blockUrl(id) : this.admin.unblockUrl(id);
    call.subscribe({
      next: () => {
        this.notifications.showInfo(action === 'block' ? 'URL blocked' : 'URL unblocked', id);
        this.action.set(null);
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.action.set(null);
        this.saving.set(false);
      }
    });
  }
}
