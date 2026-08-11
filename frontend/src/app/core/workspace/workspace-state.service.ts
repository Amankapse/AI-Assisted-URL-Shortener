import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { WorkspaceResponse } from '../api/api-types';
import { WorkspaceApi } from '../api/workspace-api.service';

@Injectable({ providedIn: 'root' })
export class WorkspaceStateService {
  private readonly api = inject(WorkspaceApi);
  private readonly workspacesSignal = signal<WorkspaceResponse[]>([]);
  private readonly selectedIdSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);

  readonly workspaces = this.workspacesSignal.asReadonly();
  readonly selectedWorkspaceId = this.selectedIdSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly selectedWorkspace = computed(() =>
    this.workspacesSignal().find((workspace) => workspace.id === this.selectedIdSignal()) ?? null
  );

  load(): void {
    this.loadingSignal.set(true);
    this.workspacesSignal.set([]);
    this.api.list().pipe(finalize(() => this.loadingSignal.set(false))).subscribe({
      next: (workspaces) => {
        this.workspacesSignal.set(workspaces);
        this.selectedIdSignal.set(workspaces.find((workspace) => workspace.defaultWorkspace)?.id ?? workspaces[0]?.id ?? null);
      },
      error: () => {
        this.workspacesSignal.set([]);
        this.selectedIdSignal.set(null);
      }
    });
  }

  select(workspaceId: string): void {
    if (this.workspacesSignal().some((workspace) => workspace.id === workspaceId)) {
      this.selectedIdSignal.set(workspaceId);
    }
  }

  clear(): void {
    this.workspacesSignal.set([]);
    this.selectedIdSignal.set(null);
    this.loadingSignal.set(false);
  }
}
