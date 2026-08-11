import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiKeyCreatedResponse, ApiKeyResponse, CreateApiKeyRequest } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class ApiKeyApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  list(workspaceId: string): Observable<ApiKeyResponse[]> {
    return this.http.get<ApiKeyResponse[]>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/api-keys`));
  }

  create(workspaceId: string, request: CreateApiKeyRequest): Observable<ApiKeyCreatedResponse> {
    return this.http.post<ApiKeyCreatedResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/api-keys`), request);
  }

  revoke(workspaceId: string, id: string): Observable<ApiKeyResponse> {
    return this.http.post<ApiKeyResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/api-keys/${id}/revoke`), {});
  }
}
