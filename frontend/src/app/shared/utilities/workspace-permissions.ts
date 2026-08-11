import { WorkspaceRole } from '../../core/api/api-types';

export function canManageCampaigns(role: WorkspaceRole | undefined | null): boolean {
  return role === 'OWNER' || role === 'ADMIN' || role === 'EDITOR';
}

export function canDeleteCampaigns(role: WorkspaceRole | undefined | null): boolean {
  return role === 'OWNER' || role === 'ADMIN';
}

export function canManageMembers(role: WorkspaceRole | undefined | null): boolean {
  return role === 'OWNER' || role === 'ADMIN';
}

export function canCreateApiKeys(role: WorkspaceRole | undefined | null): boolean {
  return role === 'OWNER' || role === 'ADMIN';
}
