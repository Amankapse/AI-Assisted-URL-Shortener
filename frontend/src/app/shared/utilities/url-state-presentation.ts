import { UrlState } from '../../core/api/api-types';

export interface UrlStatePresentation {
  label: string;
  tone: 'success' | 'muted' | 'danger' | 'warning';
  description: string;
}

export function urlStatePresentation(state: UrlState): UrlStatePresentation {
  switch (state) {
    case 'ACTIVE':
      return { label: 'Active', tone: 'success', description: 'Redirects are available.' };
    case 'DISABLED':
      return { label: 'Disabled', tone: 'muted', description: 'Owner-disabled link.' };
    case 'BLOCKED':
      return { label: 'Blocked', tone: 'danger', description: 'Unavailable due to platform moderation.' };
    case 'EXPIRED':
      return { label: 'Expired', tone: 'warning', description: 'Expiration time has passed.' };
    default:
      return { label: String(state), tone: 'muted', description: 'Current link state.' };
  }
}

export function destinationHost(originalUrl: string): string {
  try {
    return new URL(originalUrl).host;
  } catch {
    return originalUrl;
  }
}
