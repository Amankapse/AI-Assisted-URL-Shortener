import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import {
  CreateShortUrlRequest,
  Page,
  ResourceWithEtag,
  ShortUrlResponse,
  UpdateDestinationRequest,
  UpdateExpirationRequest,
  UpdateUrlCampaignRequest,
  UpdateUrlTagsRequest,
  UrlSearchParams
} from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class UrlApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  list(params: UrlSearchParams): Observable<Page<ShortUrlResponse>> {
    return this.http.get<Page<ShortUrlResponse>>(this.config.apiUrl('/api/v1/urls'), {
      params: toParams(params)
    });
  }

  create(request: CreateShortUrlRequest, idempotencyKey: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.post<ShortUrlResponse>(this.config.apiUrl('/api/v1/urls'), request, {
      headers: { 'Idempotency-Key': idempotencyKey },
      observe: 'response'
    }).pipe(map(toResource));
  }

  get(id: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.get<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}`), {
      observe: 'response'
    }).pipe(map(toResource));
  }

  updateExpiration(id: string, request: UpdateExpirationRequest): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.patch<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}`), request, {
      observe: 'response'
    }).pipe(map(toResource));
  }

  updateDestination(id: string, request: UpdateDestinationRequest, etag: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.patch<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}/destination`), request, {
      headers: { 'If-Match': etag },
      observe: 'response'
    }).pipe(map(toResource));
  }

  updateCampaign(id: string, request: UpdateUrlCampaignRequest, etag: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.patch<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}/campaign`), request, {
      headers: { 'If-Match': etag },
      observe: 'response'
    }).pipe(map(toResource));
  }

  replaceTags(id: string, request: UpdateUrlTagsRequest, etag: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.put<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}/tags`), request, {
      headers: { 'If-Match': etag },
      observe: 'response'
    }).pipe(map(toResource));
  }

  enable(id: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.post<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}/enable`), {}, {
      observe: 'response'
    }).pipe(map(toResource));
  }

  disable(id: string): Observable<ResourceWithEtag<ShortUrlResponse>> {
    return this.http.post<ShortUrlResponse>(this.config.apiUrl(`/api/v1/urls/${id}/disable`), {}, {
      observe: 'response'
    }).pipe(map(toResource));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(this.config.apiUrl(`/api/v1/urls/${id}`));
  }
}

export function toResource<T>(response: HttpResponse<T>): ResourceWithEtag<T> {
  if (response.body === null) {
    throw new Error('Expected response body.');
  }
  return { body: response.body, etag: response.headers.get('ETag') };
}

function toParams(params: UrlSearchParams): HttpParams {
  let httpParams = new HttpParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  });
  return httpParams;
}
