import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RuntimeConfigService } from '../config/runtime-config.service';
import {
  AddMemberRequest,
  CreateWorkspaceRequest,
  MemberResponse,
  UpdateMemberRoleRequest,
  WorkspaceResponse
} from './api-types';

@Injectable({ providedIn: 'root' })
export class WorkspaceApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);

  list(): Observable<WorkspaceResponse[]> {
    return this.http.get<WorkspaceResponse[]>(this.config.apiUrl('/api/v1/workspaces'));
  }

  get(id: string): Observable<WorkspaceResponse> {
    return this.http.get<WorkspaceResponse>(this.config.apiUrl(`/api/v1/workspaces/${id}`));
  }

  create(request: CreateWorkspaceRequest): Observable<WorkspaceResponse> {
    return this.http.post<WorkspaceResponse>(this.config.apiUrl('/api/v1/workspaces'), request);
  }

  members(workspaceId: string): Observable<MemberResponse[]> {
    return this.http.get<MemberResponse[]>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/members`));
  }

  addMember(workspaceId: string, request: AddMemberRequest): Observable<MemberResponse> {
    return this.http.post<MemberResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/members`), request);
  }

  updateMember(workspaceId: string, userId: string, request: UpdateMemberRoleRequest): Observable<MemberResponse> {
    return this.http.patch<MemberResponse>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/members/${userId}`), request);
  }

  removeMember(workspaceId: string, userId: string): Observable<void> {
    return this.http.delete<void>(this.config.apiUrl(`/api/v1/workspaces/${workspaceId}/members/${userId}`));
  }
}
