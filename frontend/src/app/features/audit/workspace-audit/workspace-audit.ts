import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { AuditApi } from '../../../core/api/audit-api.service';
import { AuditAction, AuditActorType, AuditEventResponse, AuditResourceType, Page } from '../../../core/api/api-types';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { WorkspaceStateService } from '../../../core/workspace/workspace-state.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { safeMetadataEntries } from '../../../shared/utilities/audit-format';

const ACTIONS: AuditAction[] = [
  'URL_CREATED', 'URL_DESTINATION_CHANGED', 'URL_EXPIRATION_CHANGED', 'URL_ENABLED', 'URL_DISABLED', 'URL_DELETED',
  'URL_BLOCKED', 'URL_UNBLOCKED', 'URL_CAMPAIGN_CHANGED', 'URL_TAGS_CHANGED', 'CAMPAIGN_CREATED', 'CAMPAIGN_UPDATED',
  'CAMPAIGN_DELETED', 'API_KEY_CREATED', 'API_KEY_REVOKED', 'WORKSPACE_CREATED', 'WORKSPACE_MEMBER_ADDED',
  'WORKSPACE_MEMBER_ROLE_CHANGED', 'WORKSPACE_MEMBER_REMOVED'
];
const RESOURCE_TYPES: AuditResourceType[] = ['URL', 'CAMPAIGN', 'WORKSPACE', 'WORKSPACE_MEMBER', 'API_KEY'];
const ACTOR_TYPES: AuditActorType[] = ['USER', 'SYSTEM', 'API_KEY', 'SERVICE'];

@Component({
  selector: 'app-workspace-audit',
  imports: [DatePipe, ReactiveFormsModule, EmptyState, ErrorState, Pagination],
  templateUrl: './workspace-audit.html',
  styleUrl: './workspace-audit.css'
})
export class WorkspaceAudit {
  private readonly api = inject(AuditApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly fb = inject(FormBuilder);

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly page = signal<Page<AuditEventResponse> | null>(null);
  readonly loading = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly actions = ACTIONS;
  readonly resourceTypes = RESOURCE_TYPES;
  readonly actorTypes = ACTOR_TYPES;
  readonly safeMetadataEntries = safeMetadataEntries;

  readonly filters = this.fb.nonNullable.group({
    from: [''],
    to: [''],
    action: [''],
    resourceType: [''],
    actorType: ['']
  });

  constructor() {
    this.filters.valueChanges.pipe(debounceTime(300)).subscribe(() => this.load(0));
    effect(() => {
      this.page.set(null);
      if (this.workspaceId()) {
        this.load(0);
      }
    });
  }

  load(page = this.page()?.number ?? 0, size = this.page()?.size ?? 20): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      return;
    }
    const value = this.filters.getRawValue();
    this.loading.set(true);
    this.problem.set(null);
    this.api.workspaceAudit(workspaceId, {
      from: value.from || null,
      to: value.to || null,
      action: value.action as AuditAction || null,
      resourceType: value.resourceType as AuditResourceType || null,
      actorType: value.actorType as AuditActorType || null,
      page,
      size
    }).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }
}
