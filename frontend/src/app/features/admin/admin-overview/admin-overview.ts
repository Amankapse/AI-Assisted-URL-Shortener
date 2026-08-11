import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnalyticsApi } from '../../../core/api/analytics-api.service';
import { AdminAnalyticsOverviewResponse, TopLinkResponse } from '../../../core/api/api-types';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { ErrorState } from '../../../shared/components/error-state/error-state';

@Component({
  selector: 'app-admin-overview',
  imports: [RouterLink, ErrorState],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.css'
})
export class AdminOverview {
  private readonly analytics = inject(AnalyticsApi);
  private readonly problems = inject(ProblemDetailsService);

  readonly overview = signal<AdminAnalyticsOverviewResponse | null>(null);
  readonly topLinks = signal<TopLinkResponse[]>([]);
  readonly loading = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.analytics.adminOverview().subscribe({
      next: (overview) => {
        this.overview.set(overview);
        this.analytics.topLinks(10).subscribe({
          next: (links) => {
            this.topLinks.set(links);
            this.loading.set(false);
          },
          error: (error) => {
            this.problem.set(this.problems.fromHttpError(error));
            this.loading.set(false);
          }
        });
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }
}
