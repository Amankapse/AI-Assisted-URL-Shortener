import { Component, computed, effect, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CampaignResponse, ShortUrlResponse, TagResponse } from '../../../core/api/api-types';
import { CampaignApi } from '../../../core/api/campaign-api.service';
import { TagApi } from '../../../core/api/tag-api.service';
import { UrlApi } from '../../../core/api/url-api.service';
import { WorkspaceStateService } from '../../../core/workspace/workspace-state.service';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { NotificationService } from '../../../core/error/notification.service';
import { CopyButton } from '../../../shared/components/copy-button/copy-button';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { TagChip } from '../../../shared/components/tag-chip/tag-chip';
import { IdempotencyKeyService } from '../../../shared/utilities/idempotency-key.service';
import { invalidTags, MAX_TAGS, normalizeTags, TAG_PATTERN } from '../../../shared/utilities/tag-utils';

@Component({
  selector: 'app-url-create',
  imports: [ReactiveFormsModule, RouterLink, CopyButton, EmptyState, ErrorState, TagChip],
  templateUrl: './url-create.html',
  styleUrl: './url-create.css'
})
export class UrlCreate {
  private readonly fb = inject(FormBuilder);
  private readonly urls = inject(UrlApi);
  private readonly campaignsApi = inject(CampaignApi);
  private readonly tagsApi = inject(TagApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);
  private readonly idempotency = inject(IdempotencyKeyService);

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly tagOptions = signal<TagResponse[]>([]);
  readonly tags = signal<string[]>([]);
  readonly loadingOptions = signal(false);
  readonly submitting = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly created = signal<ShortUrlResponse | null>(null);
  readonly idempotencyKey = signal(this.idempotency.create());
  readonly tagLimit = MAX_TAGS;
  readonly existingTagNames = computed(() => this.tagOptions().map((tag) => tag.name));

  readonly form = this.fb.nonNullable.group({
    originalUrl: ['', [Validators.required, Validators.maxLength(2048), Validators.pattern(/^https?:\/\/.+/i)]],
    customAlias: ['', [Validators.maxLength(100), Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
    expiresAt: ['', [Validators.required, futureDateTimeValidator]],
    campaignId: ['']
  });

  readonly tagControl = this.fb.nonNullable.control('', [Validators.maxLength(80), Validators.pattern(TAG_PATTERN)]);

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      this.campaigns.set([]);
      this.tagOptions.set([]);
      if (workspaceId) {
        this.loadOptions(workspaceId);
      }
    });
  }

  addTag(rawValue = this.tagControl.value): void {
    const next = normalizeTags([...this.tags(), rawValue]);
    const invalid = invalidTags(next);
    if (invalid.length || next.length > MAX_TAGS) {
      this.tagControl.markAsTouched();
      return;
    }
    this.tags.set(next);
    this.tagControl.setValue('');
  }

  removeTag(tag: string): void {
    this.tags.update((tags) => tags.filter((current) => current !== tag));
  }

  submit(): void {
    this.problem.set(null);
    this.created.set(null);
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.urls.create({
      originalUrl: value.originalUrl.trim(),
      customAlias: value.customAlias.trim() || undefined,
      expiresAt: value.expiresAt,
      campaignId: value.campaignId || null,
      tags: this.tags()
    }, this.idempotencyKey()).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (resource) => {
        this.created.set(resource.body);
        this.notifications.showInfo('Link created', resource.body.shortUrl);
      },
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  createAnother(): void {
    this.created.set(null);
    this.problem.set(null);
    this.form.reset();
    this.tags.set([]);
    this.tagControl.reset('');
    this.idempotencyKey.set(this.idempotency.create());
  }

  private loadOptions(workspaceId: string): void {
    this.loadingOptions.set(true);
    this.campaignsApi.list(workspaceId).subscribe({
      next: (campaigns) => this.campaigns.set(campaigns),
      error: () => this.campaigns.set([])
    });
    this.tagsApi.list(workspaceId).pipe(finalize(() => this.loadingOptions.set(false))).subscribe({
      next: (tags) => this.tagOptions.set(tags),
      error: () => this.tagOptions.set([])
    });
  }
}

function futureDateTimeValidator(control: AbstractControl<string>): ValidationErrors | null {
  if (!control.value) {
    return null;
  }
  const expiresAt = new Date(control.value);
  return Number.isFinite(expiresAt.getTime()) && expiresAt.getTime() > Date.now() ? null : { futureDateTime: true };
}
