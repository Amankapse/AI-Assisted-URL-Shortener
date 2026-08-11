import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CampaignResponse } from '../../core/api/api-types';
import { CampaignApi } from '../../core/api/campaign-api.service';
import { ProblemDetails } from '../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../core/error/problem-details.service';
import { NotificationService } from '../../core/error/notification.service';
import { WorkspaceStateService } from '../../core/workspace/workspace-state.service';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { canDeleteCampaigns, canManageCampaigns } from '../../shared/utilities/workspace-permissions';

@Component({
  selector: 'app-campaigns',
  imports: [ReactiveFormsModule, ConfirmDialog, EmptyState, ErrorState],
  templateUrl: './campaigns.html',
  styleUrl: './campaigns.css'
})
export class Campaigns {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CampaignApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly selectedWorkspace = this.workspaceState.selectedWorkspace;
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly editing = signal<CampaignResponse | null>(null);
  readonly confirmDelete = signal<CampaignResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', Validators.maxLength(500)]
  });

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      this.campaigns.set([]);
      this.cancelEdit();
      if (workspaceId) {
        this.load(workspaceId);
      }
    });
  }

  canManage(): boolean {
    return canManageCampaigns(this.selectedWorkspace()?.role);
  }

  canDelete(): boolean {
    return canDeleteCampaigns(this.selectedWorkspace()?.role);
  }

  startEdit(campaign: CampaignResponse): void {
    this.editing.set(campaign);
    this.form.setValue({ name: campaign.name, description: campaign.description ?? '' });
  }

  cancelEdit(): void {
    this.editing.set(null);
    this.form.reset();
  }

  submit(): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId || this.form.invalid || !this.canManage()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = { name: value.name.trim(), description: value.description.trim() || null };
    this.saving.set(true);
    this.problem.set(null);
    const editing = this.editing();
    const call = editing ? this.api.update(workspaceId, editing.id, request) : this.api.create(workspaceId, request);
    call.subscribe({
      next: (resource) => {
        if (editing) {
          this.campaigns.update((campaigns) => campaigns.map((campaign) => campaign.id === resource.body.id ? resource.body : campaign));
          this.notifications.showInfo('Campaign updated', resource.body.name);
        } else {
          this.campaigns.update((campaigns) => [...campaigns, resource.body]);
          this.notifications.showInfo('Campaign created', resource.body.name);
        }
        this.saving.set(false);
        this.cancelEdit();
      },
      error: (error) => {
        this.saving.set(false);
        this.problem.set(this.problems.fromHttpError(error));
      }
    });
  }

  deleteConfirmed(): void {
    const workspaceId = this.workspaceId();
    const campaign = this.confirmDelete();
    if (!workspaceId || !campaign || !this.canDelete()) {
      return;
    }
    this.saving.set(true);
    this.api.delete(workspaceId, campaign.id).subscribe({
      next: () => {
        this.campaigns.update((campaigns) => campaigns.filter((current) => current.id !== campaign.id));
        this.confirmDelete.set(null);
        this.notifications.showInfo('Campaign deleted', campaign.name);
        this.saving.set(false);
      },
      error: (error) => {
        this.saving.set(false);
        this.problem.set(this.problems.fromHttpError(error));
      }
    });
  }

  private load(workspaceId: string): void {
    this.loading.set(true);
    this.problem.set(null);
    this.api.list(workspaceId).subscribe({
      next: (campaigns) => {
        this.campaigns.set(campaigns);
        this.loading.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }
}
