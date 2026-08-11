import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiKeyApi } from '../../core/api/api-key-api.service';
import { ApiKeyCreatedResponse, ApiKeyResponse } from '../../core/api/api-types';
import { ProblemDetails } from '../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../core/error/problem-details.service';
import { NotificationService } from '../../core/error/notification.service';
import { WorkspaceStateService } from '../../core/workspace/workspace-state.service';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { CopyButton } from '../../shared/components/copy-button/copy-button';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { canCreateApiKeys } from '../../shared/utilities/workspace-permissions';

const SCOPES = ['links:read', 'links:write', 'analytics:read'];

@Component({
  selector: 'app-api-keys',
  imports: [DatePipe, ReactiveFormsModule, ConfirmDialog, CopyButton, EmptyState, ErrorState],
  templateUrl: './api-keys.html',
  styleUrl: './api-keys.css'
})
export class ApiKeys {
  private readonly api = inject(ApiKeyApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly selectedWorkspace = this.workspaceState.selectedWorkspace;
  readonly keys = signal<ApiKeyResponse[]>([]);
  readonly rawCreatedKey = signal<ApiKeyCreatedResponse | null>(null);
  readonly confirmRevoke = signal<ApiKeyResponse | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly scopes = SCOPES;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    expiresAt: [''],
    linksRead: [true],
    linksWrite: [false],
    analyticsRead: [false]
  });

  constructor() {
    effect(() => {
      this.keys.set([]);
      this.rawCreatedKey.set(null);
      if (this.workspaceId()) {
        this.load();
      }
    });
  }

  canManage(): boolean {
    return canCreateApiKeys(this.selectedWorkspace()?.role);
  }

  selectedScopes(): string[] {
    const value = this.form.getRawValue();
    return [
      value.linksRead ? 'links:read' : null,
      value.linksWrite ? 'links:write' : null,
      value.analyticsRead ? 'analytics:read' : null
    ].filter((scope): scope is string => !!scope);
  }

  load(): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      return;
    }
    this.loading.set(true);
    this.problem.set(null);
    this.api.list(workspaceId).subscribe({
      next: (keys) => {
        this.keys.set(keys);
        this.loading.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }

  create(): void {
    const workspaceId = this.workspaceId();
    const scopes = this.selectedScopes();
    if (!workspaceId || this.form.invalid || !scopes.length || !this.canManage()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.problem.set(null);
    this.api.create(workspaceId, {
      name: value.name.trim(),
      scopes,
      expiresAt: value.expiresAt || null
    }).subscribe({
      next: (created) => {
        this.rawCreatedKey.set(created);
        const safeKey: ApiKeyResponse = {
          id: created.id,
          name: created.name,
          prefix: created.prefix,
          scopes: created.scopes,
          createdAt: created.createdAt,
          expiresAt: created.expiresAt,
          revokedAt: null,
          lastUsedAt: null
        };
        this.keys.update((keys) => [safeKey, ...keys]);
        this.form.reset({ name: '', expiresAt: '', linksRead: true, linksWrite: false, analyticsRead: false });
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }

  acknowledgeRawKey(): void {
    this.rawCreatedKey.set(null);
    this.load();
  }

  revokeConfirmed(): void {
    const workspaceId = this.workspaceId();
    const key = this.confirmRevoke();
    if (!workspaceId || !key) {
      return;
    }
    this.saving.set(true);
    this.api.revoke(workspaceId, key.id).subscribe({
      next: (revoked) => {
        this.keys.update((keys) => keys.map((current) => current.id === revoked.id ? revoked : current));
        this.confirmRevoke.set(null);
        this.notifications.showInfo('API key revoked', revoked.name);
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }
}
