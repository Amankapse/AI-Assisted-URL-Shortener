import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AnalyticsApi } from '../../../core/api/analytics-api.service';
import { DailyRedirectsResponse, UrlAnalyticsResponse } from '../../../core/api/api-types';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';

@Component({
  selector: 'app-url-analytics',
  imports: [DatePipe, RouterLink, EmptyState, ErrorState, StatusBadge],
  templateUrl: './url-analytics.html',
  styleUrl: './url-analytics.css'
})
export class UrlAnalytics {
  private readonly api = inject(AnalyticsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly problems = inject(ProblemDetailsService);

  readonly id = this.route.snapshot.paramMap.get('id') ?? '';
  readonly loading = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly summary = signal<UrlAnalyticsResponse | null>(null);
  readonly daily = signal<DailyRedirectsResponse | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.problem.set(null);
    forkJoin({
      summary: this.api.urlAnalytics(this.id),
      daily: this.api.dailyUrlAnalytics(this.id)
    }).subscribe({
      next: ({ summary, daily }) => {
        this.summary.set(summary);
        this.daily.set(daily);
        this.loading.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }

  maxRedirects(): number {
    return Math.max(...(this.daily()?.days ?? []).map((day) => day.redirects), 0);
  }

  barWidth(redirects: number): string {
    const max = this.maxRedirects();
    return max > 0 ? `${Math.max((redirects / max) * 100, 4)}%` : '0';
  }
}
