import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { CampaignCreateRequest, CampaignResponse, CampaignUpdateRequest, ResourceWithEtag } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { toResource } from './url-api.service';

@Injectable({ providedIn: 'root' })
export class CampaignApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  list(workspaceId: string): Observable<CampaignResponse[]> {
    return this.http.get<CampaignResponse[]>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/campaigns`));
  }

  create(workspaceId: string, request: CampaignCreateRequest): Observable<ResourceWithEtag<CampaignResponse>> {
    return this.http.post<CampaignResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/campaigns`), request, {
      observe: 'response'
    }).pipe(map(toResource));
  }

  update(workspaceId: string, campaignId: string, request: CampaignUpdateRequest): Observable<ResourceWithEtag<CampaignResponse>> {
    return this.http.patch<CampaignResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/campaigns/${campaignId}`), request, {
      observe: 'response'
    }).pipe(map(toResource));
  }

  delete(workspaceId: string, campaignId: string): Observable<void> {
    return this.http.delete<void>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/campaigns/${campaignId}`));
  }
}
