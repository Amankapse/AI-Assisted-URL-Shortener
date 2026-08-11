import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { App } from './app';
import { AuthApi } from './core/api/auth-api.service';
import { AuthResponse, ShortUrlResponse, WorkspaceResponse } from './core/api/api-types';
import { RuntimeConfigService } from './core/config/runtime-config.service';
import { ProblemDetailsService } from './core/error/problem-details.service';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { workspaceInterceptor } from './core/interceptors/workspace.interceptor';
import { AuthService } from './core/auth/auth.service';
import { AuthStateService } from './core/auth/auth-state.service';
import { WorkspaceStateService } from './core/workspace/workspace-state.service';
import { UrlApi } from './core/api/url-api.service';
import { AnalyticsApi } from './core/api/analytics-api.service';
import { ApiKeyApi } from './core/api/api-key-api.service';
import { AdminApi } from './core/api/admin-api.service';
import { AuditApi } from './core/api/audit-api.service';
import { WorkspaceApi } from './core/api/workspace-api.service';
import { IdempotencyKeyService } from './shared/utilities/idempotency-key.service';
import { invalidTags, normalizeTags } from './shared/utilities/tag-utils';
import { urlStatePresentation } from './shared/utilities/url-state-presentation';
import { canCreateApiKeys, canDeleteCampaigns, canManageCampaigns, canManageMembers } from './shared/utilities/workspace-permissions';
import { safeMetadataEntries } from './shared/utilities/audit-format';

const authResponse: AuthResponse = {
  accessToken: 'access-token',
  tokenType: 'Bearer',
  expiresAt: '2026-08-11T00:05:00Z',
  user: { id: 'user-1', email: 'user@example.com', role: 'USER' }
};

const shortUrlResponse: ShortUrlResponse = {
  id: 'url-1',
  shortCode: 'abc',
  shortUrl: 'https://go.example.com/r/abc',
  customAlias: 'abc',
  originalUrl: 'https://example.com/landing',
  createdAt: '2026-08-11T00:00:00',
  expiresAt: '2026-08-12T00:00:00',
  enabled: true,
  clickCount: 0,
  state: 'ACTIVE',
  campaign: null,
  tags: ['launch']
};

describe('App', () => {
  it('should create the app shell host', () => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])]
    });

    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });
});

describe('RuntimeConfigService', () => {
  let service: RuntimeConfigService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(RuntimeConfigService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads and validates runtime configuration', async () => {
    const promise = service.load();
    http.expectOne('/app-config.json').flush({
      apiBaseUrl: 'https://api.example.com',
      publicShortUrlBase: 'https://go.example.com',
      environment: 'production'
    });
    await promise;

    expect(service.apiUrl('/api/v1/auth/me')).toBe('https://api.example.com/api/v1/auth/me');
  });

  it('rejects malformed runtime configuration', async () => {
    const promise = service.load();
    http.expectOne('/app-config.json').flush({
      apiBaseUrl: 'file:///tmp/config',
      publicShortUrlBase: 'https://go.example.com'
    });

    await expect(promise).rejects.toThrow(/apiBaseUrl/);
  });
});

describe('ProblemDetailsService', () => {
  it('preserves safe problem detail fields and retry guidance', () => {
    const service = new ProblemDetailsService();
    const error = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({ 'Retry-After': '30' }),
      error: {
        title: 'Too Many Requests',
        detail: 'Try again later.',
        errorCode: 'rate_limited',
        correlationId: 'abc-123'
      }
    });

    expect(service.fromHttpError(error)).toEqual(expect.objectContaining({
      status: 429,
      title: 'Too Many Requests',
      errorCode: 'rate_limited',
      correlationId: 'abc-123',
      retryAfterSeconds: 30
    }));
  });
});

describe('Stage 9B URL API client', () => {
  let service: UrlApi;
  let http: HttpTestingController;
  let config: RuntimeConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UrlApi);
    http = TestBed.inject(HttpTestingController);
    config = TestBed.inject(RuntimeConfigService);
    (config as unknown as { currentConfig: { set(value: unknown): void } }).currentConfig.set({
      apiBaseUrl: 'https://api.example.com',
      publicShortUrlBase: 'https://api.example.com',
      environment: 'test'
    });
  });

  afterEach(() => http.verify());

  it('sends a stable idempotency key when creating links', () => {
    let etag: string | null = null;
    service.create({
      originalUrl: 'https://example.com',
      expiresAt: '2026-08-12T00:00',
      campaignId: null,
      tags: ['launch']
    }, 'idem-123').subscribe((resource) => etag = resource.etag);

    const request = http.expectOne('https://api.example.com/api/v1/urls');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toBe('idem-123');
    request.flush(shortUrlResponse, { headers: { ETag: '"7"' } });

    expect(etag).toBe('"7"');
  });

  it('uses If-Match for guarded destination updates', () => {
    service.updateDestination('url-1', { originalUrl: 'https://example.com/new' }, '"7"').subscribe();

    const request = http.expectOne('https://api.example.com/api/v1/urls/url-1/destination');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.headers.get('If-Match')).toBe('"7"');
    request.flush(shortUrlResponse, { headers: { ETag: '"8"' } });
  });
});

describe('Stage 9B utilities', () => {
  it('normalizes, deduplicates and validates tags before URL mutations', () => {
    expect(normalizeTags([' Launch ', 'launch', 'Q3'])).toEqual(['launch', 'q3']);
    expect(invalidTags(['ok_tag', 'bad tag'])).toEqual(['bad tag']);
  });

  it('creates fresh idempotency keys for new create attempts', () => {
    const service = new IdempotencyKeyService();

    expect(service.create()).not.toBe(service.create());
  });

  it('maps URL states to accessible labels and non-color text', () => {
    expect(urlStatePresentation('BLOCKED')).toEqual(expect.objectContaining({
      label: 'Blocked',
      description: expect.stringContaining('moderation')
    }));
  });

  it('keeps campaign action affordances aligned with workspace roles', () => {
    expect(canManageCampaigns('EDITOR')).toBe(true);
    expect(canDeleteCampaigns('EDITOR')).toBe(false);
    expect(canDeleteCampaigns('ADMIN')).toBe(true);
    expect(canManageCampaigns('VIEWER')).toBe(false);
  });
});

describe('Stage 9C enterprise API clients', () => {
  let http: HttpTestingController;
  let config: RuntimeConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    config = TestBed.inject(RuntimeConfigService);
    (config as unknown as { currentConfig: { set(value: unknown): void } }).currentConfig.set({
      apiBaseUrl: 'https://api.example.com',
      publicShortUrlBase: 'https://api.example.com',
      environment: 'test'
    });
  });

  afterEach(() => http.verify());

  it('loads URL analytics and daily redirects from existing endpoints', () => {
    const api = TestBed.inject(AnalyticsApi);
    api.urlAnalytics('url-1').subscribe((value) => expect(value.totalRedirects).toBe(0));
    http.expectOne('https://api.example.com/api/v1/urls/url-1/analytics').flush({
      urlId: 'url-1',
      shortCode: 'abc',
      state: 'ACTIVE',
      totalRedirects: 0,
      lastAccessedAt: null
    });

    api.dailyUrlAnalytics('url-1').subscribe((value) => expect(value.days).toEqual([]));
    http.expectOne('https://api.example.com/api/v1/urls/url-1/analytics/daily').flush({ urlId: 'url-1', days: [] });
  });

  it('creates API keys without persisting the raw key in browser storage', () => {
    localStorage.clear();
    sessionStorage.clear();
    const api = TestBed.inject(ApiKeyApi);
    let rawKey = '';

    api.create('workspace-1', { name: 'ci', scopes: ['links:read'], expiresAt: null }).subscribe((created) => rawKey = created.rawKey);
    const request = http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/api-keys');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'ci', scopes: ['links:read'], expiresAt: null });
    request.flush({
      id: 'key-1',
      name: 'ci',
      prefix: 'usk_live',
      rawKey: 'secret-value',
      scopes: ['links:read'],
      createdAt: '2026-08-11T00:00:00',
      expiresAt: null
    });

    expect(rawKey).toBe('secret-value');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('revokes API keys and never requests secret material in the list response', () => {
    const api = TestBed.inject(ApiKeyApi);

    api.list('workspace-1').subscribe((keys) => {
      expect(keys[0]).toEqual(expect.not.objectContaining({ rawKey: expect.anything() }));
    });
    http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/api-keys').flush([{
      id: 'key-1',
      name: 'ci',
      prefix: 'usk_live',
      scopes: ['links:read'],
      createdAt: '2026-08-11T00:00:00',
      expiresAt: null,
      revokedAt: null,
      lastUsedAt: null
    }]);

    api.revoke('workspace-1', 'key-1').subscribe((key) => expect(key.revokedAt).toBeTruthy());
    const revoke = http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/api-keys/key-1/revoke');
    expect(revoke.request.method).toBe('POST');
    revoke.flush({ id: 'key-1', name: 'ci', prefix: 'usk_live', scopes: ['links:read'], createdAt: '2026-08-11T00:00:00', revokedAt: '2026-08-11T01:00:00', lastUsedAt: null });
  });

  it('uses server-side pagination and filters for workspace audit', () => {
    const api = TestBed.inject(AuditApi);
    api.workspaceAudit('workspace-1', { action: 'URL_CREATED', page: 2, size: 20 }).subscribe((page) => expect(page.number).toBe(2));

    const request = http.expectOne((req) => req.url === 'https://api.example.com/api/v1/workspaces/workspace-1/audit');
    expect(request.request.params.get('action')).toBe('URL_CREATED');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 2, numberOfElements: 0, first: false, last: true, empty: true });
  });

  it('calls workspace membership endpoints for list/add/change/remove', () => {
    const api = TestBed.inject(WorkspaceApi);
    api.members('workspace-1').subscribe((members) => expect(members.length).toBe(0));
    http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/members').flush([]);

    api.addMember('workspace-1', { email: 'new@example.com', role: 'VIEWER' }).subscribe((member) => expect(member.email).toBe('new@example.com'));
    http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/members').flush({ userId: 'user-2', email: 'new@example.com', role: 'VIEWER', defaultMembership: false });

    api.updateMember('workspace-1', 'user-2', { role: 'ANALYST' }).subscribe((member) => expect(member.role).toBe('ANALYST'));
    const update = http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/members/user-2');
    expect(update.request.method).toBe('PATCH');
    update.flush({ userId: 'user-2', email: 'new@example.com', role: 'ANALYST', defaultMembership: false });

    api.removeMember('workspace-1', 'user-2').subscribe();
    const remove = http.expectOne('https://api.example.com/api/v1/workspaces/workspace-1/members/user-2');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });

  it('keeps platform moderation and outbox operations on admin endpoints only', () => {
    const api = TestBed.inject(AdminApi);
    api.blockUrl('url-1').subscribe();
    expect(http.expectOne('https://api.example.com/api/v1/admin/urls/url-1/block').request.method).toBe('POST');

    api.unblockUrl('url-1').subscribe();
    expect(http.expectOne('https://api.example.com/api/v1/admin/urls/url-1/unblock').request.method).toBe('POST');

    api.outbox({ status: 'DEAD', page: 1, size: 20 }).subscribe((page) => expect(page.page).toBe(1));
    const outbox = http.expectOne((req) => req.url === 'https://api.example.com/api/v1/admin/outbox');
    expect(outbox.request.params.get('status')).toBe('DEAD');
    outbox.flush({ content: [], page: 1, size: 20 });
  });
});

describe('Stage 9C security utilities', () => {
  it('allowlists audit metadata and stringifies values safely', () => {
    const entries = safeMetadataEntries({
      id: 'event-1',
      occurredAt: '2026-08-11T00:00:00',
      schemaVersion: 1,
      workspaceId: 'workspace-1',
      actorType: 'USER',
      actorId: 'user-1',
      action: 'URL_CREATED',
      resourceType: 'URL',
      resourceId: 'url-1',
      correlationId: 'corr-1',
      safeMetadata: {
        shortCode: 'abc',
        rawDestinationUrl: 'https://secret.example.com',
        tags: ['launch', 'q3'],
        nested: { unsafe: true }
      }
    });

    expect(entries).toEqual([
      { key: 'shortCode', value: 'abc' },
      { key: 'tags', value: 'launch, q3' }
    ]);
  });

  it('separates workspace permissions from platform admin authority', () => {
    expect(canManageMembers('ADMIN')).toBe(true);
    expect(canCreateApiKeys('OWNER')).toBe(true);
    expect(canCreateApiKeys('ANALYST')).toBe(false);
    expect(authResponse.user.role).not.toBe('ADMIN');
  });
});

describe('AuthStateService', () => {
  it('keeps access tokens in memory only', () => {
    const state = new AuthStateService();
    localStorage.clear();
    sessionStorage.clear();

    state.setSession('memory-token', authResponse.user);

    expect(state.accessToken()).toBe('memory-token');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });
});

describe('AuthService', () => {
  it('shares one refresh request across concurrent callers', () => {
    const refreshSubject = new Subject<AuthResponse>();
    const api = {
      refreshCalls: 0,
      refresh(): Observable<AuthResponse> {
        this.refreshCalls++;
        return refreshSubject.asObservable();
      }
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        AuthStateService,
        { provide: AuthApi, useValue: api },
        { provide: WorkspaceStateService, useValue: { load: () => undefined, clear: () => undefined } },
        { provide: Router, useClass: RouterStub }
      ]
    });

    const service = TestBed.inject(AuthService);
    const firstValues: AuthResponse[] = [];
    const secondValues: AuthResponse[] = [];

    service.refresh().subscribe((value) => firstValues.push(value));
    service.refresh().subscribe((value) => secondValues.push(value));
    refreshSubject.next(authResponse);
    refreshSubject.complete();

    expect(api.refreshCalls).toBe(1);
    expect(firstValues[0]).toEqual(authResponse);
    expect(secondValues[0]).toEqual(authResponse);
  });
});

class RouterStub {
  navigate(): Promise<boolean> {
    return Promise.resolve(true);
  }
}

describe('HTTP interceptors', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let config: RuntimeConfigService;
  let authState: AuthStateService;
  let workspaceState: WorkspaceStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor, workspaceInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    config = TestBed.inject(RuntimeConfigService);
    authState = TestBed.inject(AuthStateService);
    workspaceState = TestBed.inject(WorkspaceStateService);
    (config as unknown as { currentConfig: { set(value: unknown): void } }).currentConfig.set({
      apiBaseUrl: 'https://api.example.com',
      publicShortUrlBase: 'https://api.example.com',
      environment: 'test'
    });
  });

  afterEach(() => http.verify());

  it('adds bearer auth and workspace headers only to management API calls', () => {
    authState.setSession('access-token', authResponse.user);
    (workspaceState as unknown as { selectedIdSignal: { set(value: string): void }; workspacesSignal: { set(value: WorkspaceResponse[]): void } })
      .workspacesSignal.set([{ id: 'workspace-1', name: 'Default', role: 'OWNER', defaultWorkspace: true }]);
    (workspaceState as unknown as { selectedIdSignal: { set(value: string): void } }).selectedIdSignal.set('workspace-1');

    client.get('https://api.example.com/api/v1/urls').subscribe();
    const managementRequest = http.expectOne('https://api.example.com/api/v1/urls');
    expect(managementRequest.request.headers.get('Authorization')).toBe('Bearer access-token');
    expect(managementRequest.request.headers.get('X-Workspace-ID')).toBe('workspace-1');
    managementRequest.flush({});

    client.get('https://api.example.com/actuator/health/liveness').subscribe();
    const healthRequest = http.expectOne('https://api.example.com/actuator/health/liveness');
    expect(healthRequest.request.headers.has('X-Workspace-ID')).toBeFalsy();
    healthRequest.flush({});
  });
});
