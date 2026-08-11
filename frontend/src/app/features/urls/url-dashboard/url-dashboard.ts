import { Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, finalize } from 'rxjs';
import { CampaignResponse, Page, ShortUrlResponse, TagResponse, UrlSearchParams, UrlSort } from '../../../core/api/api-types';
import { CampaignApi } from '../../../core/api/campaign-api.service';
import { TagApi } from '../../../core/api/tag-api.service';
import { UrlApi } from '../../../core/api/url-api.service';
import { WorkspaceStateService } from '../../../core/workspace/workspace-state.service';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { NotificationService } from '../../../core/error/notification.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { CopyButton } from '../../../shared/components/copy-button/copy-button';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { TagChip } from '../../../shared/components/tag-chip/tag-chip';
import { urlStatePresentation } from '../../../shared/utilities/url-state-presentation';

const SORTS: UrlSort[] = [
  'createdAt,desc',
  'createdAt,asc',
  'expiresAt,desc',
  'expiresAt,asc',
  'clickCount,desc',
  'clickCount,asc',
  'shortCode,asc',
  'shortCode,desc'
];

@Component({
  selector: 'app-url-dashboard',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    RouterLink,
    ConfirmDialog,
    CopyButton,
    EmptyState,
    ErrorState,
    Pagination,
    StatusBadge,
    TagChip
  ],
  templateUrl: './url-dashboard.html',
  styleUrl: './url-dashboard.css'
})
export class UrlDashboard {
  private readonly fb = inject(FormBuilder);
  private readonly urls = inject(UrlApi);
  private readonly campaignsApi = inject(CampaignApi);
  private readonly tagsApi = inject(TagApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);
  private syncing = false;
  private urlRequestId = 0;

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly campaigns = signal<CampaignResponse[]>([]);
  readonly tags = signal<TagResponse[]>([]);
  readonly page = signal<Page<ShortUrlResponse> | null>(null);
  readonly loading = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly actionId = signal<string | null>(null);
  readonly confirmDelete = signal<ShortUrlResponse | null>(null);
  readonly statePresentation = urlStatePresentation;
  readonly sortOptions = SORTS;

  readonly filters = this.fb.nonNullable.group({
    q: [''],
    state: [''],
    campaignId: [''],
    tag: [''],
    createdFrom: [''],
    createdTo: [''],
    expiresBefore: [''],
    expiresAfter: [''],
    customAlias: [''],
    sort: ['createdAt,desc' as UrlSort]
  });

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      this.syncing = true;
      this.filters.patchValue({
        q: params.get('q') ?? '',
        state: params.get('state') ?? '',
        campaignId: params.get('campaignId') ?? '',
        tag: params.get('tag') ?? '',
        createdFrom: params.get('createdFrom') ?? '',
        createdTo: params.get('createdTo') ?? '',
        expiresBefore: params.get('expiresBefore') ?? '',
        expiresAfter: params.get('expiresAfter') ?? '',
        customAlias: params.get('customAlias') ?? '',
        sort: this.safeSort(params.get('sort'))
      }, { emitEvent: false });
      this.syncing = false;
      this.loadUrls();
    });

    this.filters.valueChanges.pipe(debounceTime(320)).subscribe(() => {
      if (!this.syncing) {
        this.router.navigate([], { relativeTo: this.route, queryParams: this.queryParams(0), replaceUrl: true });
      }
    });

    effect(() => {
      const workspaceId = this.workspaceId();
      this.campaigns.set([]);
      this.tags.set([]);
      this.page.set(null);
      if (workspaceId) {
        this.loadMetadata(workspaceId);
        this.loadUrls();
      }
    });
  }

  host(url: string): string {
    try {
      return new URL(url).host;
    } catch {
      return url;
    }
  }

  changePage(page: number): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: this.queryParams(page), replaceUrl: true });
  }

  changePageSize(size: number): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { ...this.queryParams(0), size }, replaceUrl: true });
  }

  clearFilters(): void {
    this.filters.reset({ sort: 'createdAt,desc' });
  }

  disable(link: ShortUrlResponse): void {
    this.mutate(link.id, () => this.urls.disable(link.id), 'Link disabled');
  }

  enable(link: ShortUrlResponse): void {
    this.mutate(link.id, () => this.urls.enable(link.id), 'Link enabled');
  }

  deleteConfirmed(): void {
    const link = this.confirmDelete();
    if (!link) {
      return;
    }
    this.actionId.set(link.id);
    this.urls.delete(link.id).pipe(finalize(() => this.actionId.set(null))).subscribe({
      next: () => {
        this.confirmDelete.set(null);
        this.notifications.showInfo('Link deleted', link.shortCode);
        this.loadUrls();
      },
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  ownerActionsAllowed(link: ShortUrlResponse): boolean {
    return link.state !== 'BLOCKED';
  }

  private mutate(id: string, request: () => ReturnType<UrlApi['enable']>, message: string): void {
    this.actionId.set(id);
    request().pipe(finalize(() => this.actionId.set(null))).subscribe({
      next: (resource) => {
        this.replaceLink(resource.body);
        this.notifications.showInfo(message, resource.body.shortCode);
      },
      error: (error) => this.problem.set(this.problems.fromHttpError(error))
    });
  }

  private replaceLink(link: ShortUrlResponse): void {
    const currentPage = this.page();
    if (!currentPage) {
      return;
    }
    this.page.set({ ...currentPage, content: currentPage.content.map((current) => current.id === link.id ? link : current) });
  }

  private loadUrls(): void {
    if (!this.workspaceId()) {
      return;
    }
    const requestId = ++this.urlRequestId;
    this.loading.set(true);
    this.problem.set(null);
    this.urls.list(this.searchParams()).pipe(finalize(() => {
      if (requestId === this.urlRequestId) {
        this.loading.set(false);
      }
    })).subscribe({
      next: (page) => {
        if (requestId === this.urlRequestId) {
          this.page.set(page);
        }
      },
      error: (error) => {
        if (requestId === this.urlRequestId) {
          this.page.set(null);
          this.problem.set(this.problems.fromHttpError(error));
        }
      }
    });
  }

  private loadMetadata(workspaceId: string): void {
    this.campaignsApi.list(workspaceId).subscribe({
      next: (campaigns) => this.campaigns.set(campaigns),
      error: () => this.campaigns.set([])
    });
    this.tagsApi.list(workspaceId).subscribe({
      next: (tags) => this.tags.set(tags),
      error: () => this.tags.set([])
    });
  }

  private searchParams(): UrlSearchParams {
    const routeParams = this.route.snapshot.queryParamMap;
    const value = this.filters.getRawValue();
    return {
      ...this.queryParams(Number(routeParams.get('page') ?? 0)),
      size: Number(routeParams.get('size') ?? 20),
      sort: this.safeSort(value.sort)
    };
  }

  private queryParams(page: number): UrlSearchParams {
    const value = this.filters.getRawValue();
    return {
      q: value.q.trim() || null,
      state: value.state || null,
      campaignId: value.campaignId || null,
      tag: value.tag || null,
      createdFrom: value.createdFrom || null,
      createdTo: value.createdTo || null,
      expiresBefore: value.expiresBefore || null,
      expiresAfter: value.expiresAfter || null,
      customAlias: value.customAlias === '' ? null : value.customAlias === 'true',
      sort: this.safeSort(value.sort),
      page,
      size: Number(this.route.snapshot.queryParamMap.get('size') ?? 20)
    };
  }

  private safeSort(value: string | null | undefined): UrlSort {
    return SORTS.includes(value as UrlSort) ? value as UrlSort : 'createdAt,desc';
  }
}
