import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OutboxPageResponse, OutboxSearchParams } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  blockUrl(id: string): Observable<void> {
    return this.http.post<void>(this.config.apiUrl(`/api/v1/admin/urls/${id}/block`), {});
  }

  unblockUrl(id: string): Observable<void> {
    return this.http.post<void>(this.config.apiUrl(`/api/v1/admin/urls/${id}/unblock`), {});
  }

  outbox(params: OutboxSearchParams): Observable<OutboxPageResponse> {
    return this.http.get<OutboxPageResponse>(this.config.apiUrl('/api/v1/admin/outbox'), {
      params: toParams(params)
    });
  }
}

function toParams(params: OutboxSearchParams): HttpParams {
  let httpParams = new HttpParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  });
  return httpParams;
}
