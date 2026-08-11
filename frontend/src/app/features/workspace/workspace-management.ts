import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkspaceApi } from '../../core/api/workspace-api.service';
import { MemberResponse, WorkspaceResponse, WorkspaceRole } from '../../core/api/api-types';
import { ProblemDetails } from '../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../core/error/problem-details.service';
import { NotificationService } from '../../core/error/notification.service';
import { WorkspaceStateService } from '../../core/workspace/workspace-state.service';
import { ConfirmDialog } from '../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../shared/components/error-state/error-state';
import { canManageMembers } from '../../shared/utilities/workspace-permissions';

const ROLES: WorkspaceRole[] = ['OWNER', 'ADMIN', 'EDITOR', 'ANALYST', 'VIEWER'];

@Component({
  selector: 'app-workspace-management',
  imports: [ReactiveFormsModule, ConfirmDialog, EmptyState, ErrorState],
  templateUrl: './workspace-management.html',
  styleUrl: './workspace-management.css'
})
export class WorkspaceManagement {
  private readonly api = inject(WorkspaceApi);
  private readonly workspaceState = inject(WorkspaceStateService);
  private readonly problems = inject(ProblemDetailsService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly workspaceId = this.workspaceState.selectedWorkspaceId;
  readonly selectedWorkspace = this.workspaceState.selectedWorkspace;
  readonly workspaces = this.workspaceState.workspaces;
  readonly roles = ROLES;
  readonly members = signal<MemberResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly confirmRemove = signal<MemberResponse | null>(null);
  readonly confirmDemote = signal<{ member: MemberResponse; role: WorkspaceRole } | null>(null);

  readonly workspaceForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(160)]]
  });
  readonly memberForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['VIEWER' as WorkspaceRole, Validators.required]
  });

  constructor() {
    effect(() => {
      this.members.set([]);
      if (this.workspaceId()) {
        this.loadMembers();
      }
    });
  }

  canManage(): boolean {
    return canManageMembers(this.selectedWorkspace()?.role);
  }

  createWorkspace(): void {
    if (this.workspaceForm.invalid) {
      this.workspaceForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.api.create({ name: this.workspaceForm.getRawValue().name.trim() }).subscribe({
      next: (workspace) => {
        this.notifications.showInfo('Workspace created', workspace.name);
        this.workspaceForm.reset();
        this.workspaceState.load();
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }

  loadMembers(): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      return;
    }
    this.loading.set(true);
    this.problem.set(null);
    this.api.members(workspaceId).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.loading.set(false);
      }
    });
  }

  addMember(): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId || this.memberForm.invalid || !this.canManage()) {
      this.memberForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const value = this.memberForm.getRawValue();
    this.api.addMember(workspaceId, { email: value.email.trim(), role: value.role }).subscribe({
      next: (member) => {
        this.members.update((members) => [...members, member]);
        this.memberForm.reset({ email: '', role: 'VIEWER' });
        this.notifications.showInfo('Member added', member.email);
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }

  requestRoleChange(member: MemberResponse, role: WorkspaceRole): void {
    if (member.role === role) {
      return;
    }
    if ((member.role === 'OWNER' || member.role === 'ADMIN') && (role === 'EDITOR' || role === 'ANALYST' || role === 'VIEWER')) {
      this.confirmDemote.set({ member, role });
      return;
    }
    this.updateRole(member, role);
  }

  updateRole(member: MemberResponse, role: WorkspaceRole): void {
    const workspaceId = this.workspaceId();
    if (!workspaceId || !this.canManage()) {
      return;
    }
    this.saving.set(true);
    this.api.updateMember(workspaceId, member.userId, { role }).subscribe({
      next: (updated) => {
        this.members.update((members) => members.map((current) => current.userId === updated.userId ? updated : current));
        this.confirmDemote.set(null);
        this.notifications.showInfo('Member role updated', updated.email);
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }

  removeConfirmed(): void {
    const workspaceId = this.workspaceId();
    const member = this.confirmRemove();
    if (!workspaceId || !member || !this.canManage()) {
      return;
    }
    this.saving.set(true);
    this.api.removeMember(workspaceId, member.userId).subscribe({
      next: () => {
        this.members.update((members) => members.filter((current) => current.userId !== member.userId));
        this.confirmRemove.set(null);
        this.notifications.showInfo('Member removed', member.email);
        this.saving.set(false);
      },
      error: (error) => {
        this.problem.set(this.problems.fromHttpError(error));
        this.saving.set(false);
      }
    });
  }

  workspaceLabel(workspace: WorkspaceResponse): string {
    return `${workspace.name} (${workspace.role})`;
  }
}
