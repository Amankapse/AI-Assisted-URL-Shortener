import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminAnalyticsOverviewResponse,
  DailyRedirectsResponse,
  TopLinkResponse,
  UrlAnalyticsResponse
} from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class AnalyticsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  urlAnalytics(id: string): Observable<UrlAnalyticsResponse> {
    return this.http.get<UrlAnalyticsResponse>(this.config.apiUrl(`/api/v1/urls/${id}/analytics`));
  }

  dailyUrlAnalytics(id: string): Observable<DailyRedirectsResponse> {
    return this.http.get<DailyRedirectsResponse>(this.config.apiUrl(`/api/v1/urls/${id}/analytics/daily`));
  }

  adminOverview(): Observable<AdminAnalyticsOverviewResponse> {
    return this.http.get<AdminAnalyticsOverviewResponse>(this.config.apiUrl('/api/v1/admin/analytics/overview'));
  }

  topLinks(limit = 10): Observable<TopLinkResponse[]> {
    return this.http.get<TopLinkResponse[]>(this.config.apiUrl('/api/v1/admin/analytics/top-links'), {
      params: new HttpParams().set('limit', String(limit))
    });
  }
}
