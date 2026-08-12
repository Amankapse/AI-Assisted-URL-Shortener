export type UserRole = 'USER' | 'ADMIN';
export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'EDITOR' | 'ANALYST' | 'VIEWER';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: UserResponse;
}

export interface WorkspaceResponse {
  id: string;
  name: string;
  role: WorkspaceRole;
  defaultWorkspace: boolean;
}

export interface CreateWorkspaceRequest {
  name: string;
}

export interface MemberResponse {
  userId: string;
  email: string;
  role: WorkspaceRole;
  defaultMembership: boolean;
}

export interface AddMemberRequest {
  email: string;
  role: WorkspaceRole;
}

export interface UpdateMemberRoleRequest {
  role: WorkspaceRole;
}

export interface ResourceWithEtag<T> {
  body: T;
  etag: string | null;
}

export type UrlState = 'ACTIVE' | 'DISABLED' | 'BLOCKED' | 'EXPIRED' | string;

export interface CampaignSummaryResponse {
  id: string;
  name: string;
  normalizedName: string;
}

export interface CampaignResponse extends CampaignSummaryResponse {
  workspaceId: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TagResponse {
  id: string;
  name: string;
  normalizedName: string;
  createdAt: string;
}

export interface ShortUrlResponse {
  id: string;
  shortCode: string;
  shortUrl: string;
  customAlias?: string | null;
  originalUrl: string;
  createdAt: string;
  expiresAt: string;
  enabled: boolean;
  clickCount: number;
  state: UrlState;
  campaign?: CampaignSummaryResponse | null;
  tags: string[];
}

export interface CreateShortUrlRequest {
  originalUrl: string;
  customAlias?: string;
  expiresAt: string;
  campaignId?: string | null;
  tags?: string[];
}

export interface UpdateDestinationRequest {
  originalUrl: string;
}

export interface UpdateExpirationRequest {
  expiresAt: string;
}

export interface UpdateUrlCampaignRequest {
  campaignId?: string | null;
}

export interface UpdateUrlTagsRequest {
  tags: string[];
}

export interface CampaignCreateRequest {
  name: string;
  description?: string | null;
}

export interface CampaignUpdateRequest {
  name: string;
  description?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface UrlAnalyticsResponse {
  urlId: string;
  shortCode: string;
  state: string;
  totalRedirects: number;
  lastAccessedAt?: string | null;
}

export interface DailyRedirectsResponse {
  urlId: string;
  days: DailyRedirects[];
}

export interface DailyRedirects {
  day: string;
  redirects: number;
}

export interface AdminAnalyticsOverviewResponse {
  totalUsers: number;
  totalLinks: number;
  activeLinks: number;
  disabledLinks: number;
  expiredLinks: number;
  totalRedirects: number;
}

export interface TopLinkResponse {
  shortCode: string;
  redirects: number;
}

export type AuditActorType = 'USER' | 'SYSTEM' | 'API_KEY' | 'SERVICE';
export type AuditResourceType = 'URL' | 'CAMPAIGN' | 'WORKSPACE' | 'WORKSPACE_MEMBER' | 'API_KEY' | 'SITE_SETTINGS' | 'CONTENT_PAGE' | 'ANNOUNCEMENT' | 'MEDIA_ASSET' | 'USER';
export type AuditAction =
  | 'URL_CREATED'
  | 'URL_DESTINATION_CHANGED'
  | 'URL_EXPIRATION_CHANGED'
  | 'URL_ENABLED'
  | 'URL_DISABLED'
  | 'URL_DELETED'
  | 'URL_BLOCKED'
  | 'URL_UNBLOCKED'
  | 'URL_CAMPAIGN_CHANGED'
  | 'URL_TAGS_CHANGED'
  | 'CAMPAIGN_CREATED'
  | 'CAMPAIGN_UPDATED'
  | 'CAMPAIGN_DELETED'
  | 'API_KEY_CREATED'
  | 'API_KEY_REVOKED'
  | 'SITE_SETTINGS_UPDATED'
  | 'CONTENT_PAGE_UPDATED'
  | 'CONTENT_PAGE_PUBLISHED'
  | 'ANNOUNCEMENT_CREATED'
  | 'ANNOUNCEMENT_UPDATED'
  | 'ANNOUNCEMENT_DISABLED'
  | 'MEDIA_ASSET_REGISTERED'
  | 'MEDIA_ASSET_UPDATED'
  | 'ADMIN_PROMOTED'
  | 'WORKSPACE_CREATED'
  | 'WORKSPACE_MEMBER_ADDED'
  | 'WORKSPACE_MEMBER_ROLE_CHANGED'
  | 'WORKSPACE_MEMBER_REMOVED';

export interface AuditEventResponse {
  id: string;
  occurredAt: string;
  schemaVersion: number;
  workspaceId?: string | null;
  actorType: AuditActorType;
  actorId?: string | null;
  action: AuditAction;
  resourceType: AuditResourceType;
  resourceId?: string | null;
  correlationId?: string | null;
  safeMetadata: Record<string, unknown>;
}

export interface AuditSearchParams {
  workspaceId?: string | null;
  from?: string | null;
  to?: string | null;
  action?: AuditAction | null;
  resourceType?: AuditResourceType | null;
  actorType?: AuditActorType | null;
  page?: number;
  size?: number;
}

export interface CreateApiKeyRequest {
  name: string;
  scopes: string[];
  expiresAt?: string | null;
}

export interface ApiKeyCreatedResponse {
  id: string;
  name: string;
  prefix: string;
  rawKey: string;
  scopes: string[];
  createdAt: string;
  expiresAt?: string | null;
}

export interface ApiKeyResponse {
  id: string;
  name: string;
  prefix: string;
  scopes: string[];
  createdAt: string;
  expiresAt?: string | null;
  revokedAt?: string | null;
  lastUsedAt?: string | null;
}

export type OutboxStatus = 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'DEAD';
export type OutboxEventType =
  | 'URL_CREATED'
  | 'URL_DESTINATION_CHANGED'
  | 'URL_EXPIRATION_CHANGED'
  | 'URL_ENABLED'
  | 'URL_DISABLED'
  | 'URL_DELETED'
  | 'URL_BLOCKED'
  | 'URL_UNBLOCKED'
  | 'URL_CACHE_INVALIDATION_REQUIRED'
  | 'CLICK_RECORDED';

export interface OutboxEventSummaryResponse {
  eventId: string;
  eventType: OutboxEventType | string;
  eventVersion: number;
  aggregateType: string;
  status: OutboxStatus | string;
  attemptCount: number;
  createdAt: string;
  nextAttemptAt?: string | null;
  claimedAt?: string | null;
  processedAt?: string | null;
  lastErrorCode?: string | null;
}

export interface OutboxPageResponse {
  content: OutboxEventSummaryResponse[];
  page: number;
  size: number;
}

export type ContentPageKey = 'ABOUT' | 'FEATURES' | 'SECURITY' | 'HELP' | 'CONTACT' | 'PRIVACY' | 'TERMS' | 'ACCESSIBILITY' | 'DISCLAIMER';
export type ContentStatus = 'DRAFT' | 'PUBLISHED';
export type AnnouncementSeverity = 'INFO' | 'NOTICE' | 'WARNING' | 'MAINTENANCE';
export type AnnouncementAudience = 'PUBLIC' | 'AUTHENTICATED' | 'ADMIN';

export interface PublicMediaAssetResponse {
  id: string;
  url: string;
  altText: string;
}

export interface SiteSettingsResponse {
  brandName: string;
  tagline: string;
  supportEmail?: string | null;
  supportUrl?: string | null;
  contactText?: string | null;
  logo?: PublicMediaAssetResponse | null;
  logoDark?: PublicMediaAssetResponse | null;
  favicon?: PublicMediaAssetResponse | null;
  loginBackground?: PublicMediaAssetResponse | null;
  landingHero?: PublicMediaAssetResponse | null;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  footerDescription: string;
  footerCopyright: string;
  version: number;
}

export interface SiteSettingsUpdateRequest {
  brandName: string;
  tagline: string;
  supportEmail?: string | null;
  supportUrl?: string | null;
  contactText?: string | null;
  logoAssetId?: string | null;
  logoDarkAssetId?: string | null;
  faviconAssetId?: string | null;
  loginBackgroundAssetId?: string | null;
  landingHeroAssetId?: string | null;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  footerDescription: string;
  footerCopyright: string;
}

export interface PublicContentPageResponse {
  pageKey: ContentPageKey;
  title: string;
  summary: string;
  content: string;
  publishedAt?: string | null;
}

export interface AdminContentPageResponse extends PublicContentPageResponse {
  id: string;
  status: ContentStatus;
  version: number;
  updatedAt: string;
}

export interface ContentPageUpdateRequest {
  title: string;
  summary: string;
  content: string;
  status: ContentStatus;
}

export interface AnnouncementResponse {
  id: string;
  title: string;
  message: string;
  severity: AnnouncementSeverity;
  audience: AnnouncementAudience;
  enabled: boolean;
  startAt?: string | null;
  endAt?: string | null;
  dismissible: boolean;
  version: number;
}

export interface AnnouncementRequest {
  title: string;
  message: string;
  severity: AnnouncementSeverity;
  audience: AnnouncementAudience;
  enabled: boolean;
  startAt?: string | null;
  endAt?: string | null;
  dismissible: boolean;
}

export interface MediaAssetResponse {
  id: string;
  name: string;
  url: string;
  altText: string;
  sourceName?: string | null;
  sourceUrl?: string | null;
  license?: string | null;
  attribution?: string | null;
  enabled: boolean;
  version: number;
}

export interface MediaAssetRequest {
  name: string;
  url: string;
  altText: string;
  sourceName?: string | null;
  sourceUrl?: string | null;
  license?: string | null;
  attribution?: string | null;
  enabled: boolean;
}

export interface OutboxSearchParams {
  status?: OutboxStatus | null;
  eventType?: OutboxEventType | null;
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
}

export interface UrlSearchParams {
  q?: string | null;
  state?: UrlState | null;
  campaignId?: string | null;
  tag?: string | null;
  createdFrom?: string | null;
  createdTo?: string | null;
  expiresBefore?: string | null;
  expiresAfter?: string | null;
  customAlias?: boolean | null;
  sort?: UrlSort;
  page?: number;
  size?: number;
}

export type UrlSort = 'createdAt,desc' | 'createdAt,asc' | 'expiresAt,desc' | 'expiresAt,asc' | 'clickCount,desc' | 'clickCount,asc' | 'shortCode,asc' | 'shortCode,desc';
