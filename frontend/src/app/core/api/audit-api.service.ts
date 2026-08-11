import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEventResponse, AuditSearchParams, Page } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class AuditApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  workspaceAudit(workspaceId: string, params: AuditSearchParams): Observable<Page<AuditEventResponse>> {
    return this.http.get<Page<AuditEventResponse>>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/audit`), {
      params: toParams(params)
    });
  }

  urlAudit(urlId: string, params: AuditSearchParams): Observable<Page<AuditEventResponse>> {
    return this.http.get<Page<AuditEventResponse>>(this.config.apiUrl(`/api/v1/urls/${urlId}/audit`), {
      params: toParams(params)
    });
  }

  adminAudit(params: AuditSearchParams): Observable<Page<AuditEventResponse>> {
    return this.http.get<Page<AuditEventResponse>>(this.config.apiUrl('/api/v1/admin/audit'), {
      params: toParams(params)
    });
  }
}

function toParams(params: AuditSearchParams): HttpParams {
  let httpParams = new HttpParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  });
  return httpParams;
}
