import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TagResponse } from './api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

@Injectable({ providedIn: 'root' })
export class TagApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  list(workspaceId: string): Observable<TagResponse[]> {
    return this.http.get<TagResponse[]>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/tags`));
  }
}
