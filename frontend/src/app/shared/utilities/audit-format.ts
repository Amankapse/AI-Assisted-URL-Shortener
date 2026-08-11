import { AuditEventResponse } from '../../core/api/api-types';

const SAFE_METADATA_KEYS = new Set([
  'shortCode',
  'campaignId',
  'campaignName',
  'tagCount',
  'tags',
  'enabled',
  'expiresAt',
  'workspaceId',
  'memberUserId',
  'role',
  'previousRole',
  'apiKeyPrefix',
  'scopeCount'
]);

export function safeMetadataEntries(event: AuditEventResponse): Array<{ key: string; value: string }> {
  return Object.entries(event.safeMetadata ?? {})
    .filter(([key]) => SAFE_METADATA_KEYS.has(key))
    .map(([key, value]) => ({ key, value: stringifyMetadataValue(value) }));
}

function stringifyMetadataValue(value: unknown): string {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).join(', ');
  }
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value === 'object') {
    return '[structured value]';
  }
  return String(value);
}
