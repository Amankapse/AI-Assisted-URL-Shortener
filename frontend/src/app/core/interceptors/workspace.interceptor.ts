import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { RuntimeConfigService } from '../config/runtime-config.service';
import { WorkspaceStateService } from '../workspace/workspace-state.service';
import { SKIP_WORKSPACE } from './http-context-tokens';

export const workspaceInterceptor: HttpInterceptorFn = (request, next) => {
  const config = inject(RuntimeConfigService);
  const workspaceState = inject(WorkspaceStateService);
  const workspaceId = workspaceState.selectedWorkspaceId();

  if (!workspaceId || request.context.get(SKIP_WORKSPACE) || !isManagementApi(config, request.url)) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { 'X-Workspace-ID': workspaceId } }));
};

function isManagementApi(config: RuntimeConfigService, url: string): boolean {
  const apiBase = config.requireConfig().apiBaseUrl.replace(/\/+$/, '');
  return url.startsWith(`${apiBase}/api/v1/urls`) || url.startsWith(`${apiBase}/api/v1/workspaces`);
}
