import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CampaignResponse, ShortUrlResponse, TagResponse } from '../../../core/api/api-types';
import { CampaignApi } from '../../../core/api/campaign-api.service';
import { TagApi } from '../../../core/api/tag-api.service';
import { UrlApi } from '../../../core/api/url-api.service';
import { AuditApi } from '../../../core/api/audit-api.service';
import { AuditEventResponse, Page } from '../../../core/api/api-types';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { NotificationService } from '../../../core/error/notification.service';
import { WorkspaceStateService } from '../../../core/workspace/workspace-state.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { CopyButton } from '../../../shared/components/copy-button/copy-button';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { TagChip } from '../../../shared/components/tag-chip/tag-chip';
import { invalidTags, MAX_TAGS, normalizeTags, TAG_PATTERN } from '../../../shared/utilities/tag-utils';
import { safeMetadataEntries } from '../../../shared/utilities/audit-format';

@Component({
  selector: 'app-url-details',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ConfirmDialog, CopyButton, EmptyState, ErrorState, Pagination, StatusBadge, TagChip],
  templateUrl: './url-details.html',
  styleUrl: './url-details.css'
})
export class UrlDetails {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly urls = inject(UrlApi);
  private readonly campaignsApi = inject(CampaignApi);
  private readonly tagsApi = inject(TagApi);
  private readonly auditApi = inject(AuditApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);

  readonly id = this.route.snapshot.paramMap.get('id') ?? '';
  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly link = signal<ShortUrlResponse | null>(null);
  readonly etag = signal<string | null>(null);
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly tagOptions = signal<TagResponse[]>([]);
  readonly tags = signal<string[]>([]);
  readonly loading = signal(false);
  readonly saving = signal<string | null>(null);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly conflict = signal<ProblemDetails | null>(null);
  readonly confirmDelete = signal(false);
  readonly tagLimit = MAX_TAGS;
  readonly auditPage = signal<Page<AuditEventResponse> | null>(null);
  readonly auditProblem = signal<ProblemDetails | null>(null);
  readonly safeMetadataEntries = safeMetadataEntries;

  readonly destinationForm = this.fb.nonNullable.group({
    originalUrl: ['', [Validators.required, Validators.maxLength(2048), Validators.pattern(/^https?:\/\/.+/i)]]
  });
  readonly expirationForm = this.fb.nonNullable.group({
    expiresAt: ['', Validators.required]
  });
  readonly campaignForm = this.fb.nonNullable.group({
    campaignId: ['']
  });
  readonly tagControl = this.fb.nonNullable.control('', [Validators.maxLength(80), Validators.pattern(TAG_PATTERN)]);

  constructor() {
    this.load();
    this.loadAudit();
    effect(() => {
      const workspaceId = this.workspaceId();
      this.campaigns.set([]);
      this.tagOptions.set([]);
      if (workspaceId) {
        this.loadMetadata(workspaceId);
      }
    });
  }

  loadAudit(page = this.auditPage()?.number ?? 0, size = this.auditPage()?.size ?? 10): void {
    if (!this.id) {
      return;
    }
    this.auditProblem.set(null);
    this.auditApi.urlAudit(this.id, { page, size }).subscribe({
      next: (result) => this.auditPage.set(result),
      error: (error) => this.auditProblem.set(this.problems.fromHttpError(error))
    });
  }

  load(): void {
    if (!this.id) {
      return;
    }
    this.loading.set(true);
    this.problem.set(null);
    this.conflict.set(null);
    this.urls.get(this.id).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (resource) => this.applyResource(resource.body, resource.etag),
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  addTag(rawValue = this.tagControl.value): void {
    const next = normalizeTags([...this.tags(), rawValue]);
    if (invalidTags(next).length || next.length > MAX_TAGS) {
      this.tagControl.markAsTouched();
      return;
    }
    this.tags.set(next);
    this.tagControl.setValue('');
  }

  removeTag(tag: string): void {
    this.tags.update((tags) => tags.filter((current) => current !== tag));
  }

  saveDestination(): void {
    if (this.destinationForm.invalid) {
      this.destinationForm.markAllAsTouched();
      return;
    }
    this.withEtag('destination', (etag) => this.urls.updateDestination(this.id, {
      originalUrl: this.destinationForm.getRawValue().originalUrl.trim()
    }, etag), 'Destination updated');
  }

  saveExpiration(): void {
    if (this.expirationForm.invalid) {
      this.expirationForm.markAllAsTouched();
      return;
    }
    this.saving.set('expiration');
    this.urls.updateExpiration(this.id, {
      expiresAt: this.expirationForm.getRawValue().expiresAt
    }).pipe(finalize(() => this.saving.set(null))).subscribe({
      next: (resource) => this.applySuccess(resource.body, resource.etag, 'Expiration updated'),
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  saveCampaign(): void {
    this.withEtag('campaign', (etag) => this.urls.updateCampaign(this.id, {
      campaignId: this.campaignForm.getRawValue().campaignId || null
    }, etag), 'Campaign updated');
  }

  saveTags(): void {
    this.withEtag('tags', (etag) => this.urls.replaceTags(this.id, { tags: this.tags() }, etag), 'Tags updated');
  }

  disable(): void {
    this.saving.set('state');
    this.urls.disable(this.id).pipe(finalize(() => this.saving.set(null))).subscribe({
      next: (resource) => this.applySuccess(resource.body, resource.etag, 'Link disabled'),
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  enable(): void {
    this.saving.set('state');
    this.urls.enable(this.id).pipe(finalize(() => this.saving.set(null))).subscribe({
      next: (resource) => this.applySuccess(resource.body, resource.etag, 'Link enabled'),
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  deleteConfirmed(): void {
    this.saving.set('delete');
    this.urls.delete(this.id).pipe(finalize(() => this.saving.set(null))).subscribe({
      next: () => this.router.navigate(['/app/urls']),
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  ownerActionsAllowed(): boolean {
    return this.link()?.state !== 'BLOCKED';
  }

  private withEtag(
    key: string,
    request: (etag: string) => ReturnType<UrlApi['updateDestination']>,
    message: string
  ): void {
    const etag = this.etag();
    if (!etag) {
      this.conflict.set({ status: 428, title: 'Reload required', detail: 'The latest link version is required before saving.' });
      return;
    }
    this.saving.set(key);
    this.conflict.set(null);
    request(etag).pipe(finalize(() => this.saving.set(null))).subscribe({
      next: (resource) => this.applySuccess(resource.body, resource.etag, message),
      error: (error: HttpErrorResponse) => {
        const problem = this.problems.fromHttpError(error);
        if (error.status === 412 || error.status === 428) {
          this.conflict.set(problem);
        } else {
          this.problem.set(problem);
        }
      }
    });
  }

  private applySuccess(link: ShortUrlResponse, etag: string | null, message: string): void {
    this.applyResource(link, etag);
    this.notifications.showInfo(message, link.shortCode);
  }

  private applyResource(link: ShortUrlResponse, etag: string | null): void {
    this.link.set(link);
    this.etag.set(etag);
    this.destinationForm.setValue({ originalUrl: link.originalUrl });
    this.expirationForm.setValue({ expiresAt: link.expiresAt.slice(0, 16) });
    this.campaignForm.setValue({ campaignId: link.campaign?.id ?? '' });
    this.tags.set(normalizeTags(link.tags ?? []));
  }

  private loadMetadata(workspaceId: string): void {
    this.campaignsApi.list(workspaceId).subscribe({
      next: (campaigns) => this.campaigns.set(campaigns),
      error: () => this.campaigns.set([])
    });
    this.tagsApi.list(workspaceId).subscribe({
      next: (tags) => this.tagOptions.set(tags),
      error: () => this.tagOptions.set([])
    });
  }
}
