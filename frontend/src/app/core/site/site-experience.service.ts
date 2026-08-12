import { HttpClient, HttpHeaders } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import {
  AdminContentPageResponse,
  AnnouncementAudience,
  AnnouncementRequest,
  AnnouncementResponse,
  ContentPageKey,
  ContentPageUpdateRequest,
  MediaAssetRequest,
  MediaAssetResponse,
  PublicContentPageResponse,
  ResourceWithEtag,
  SiteSettingsResponse,
  SiteSettingsUpdateRequest
} from '../api/api-types';
import { RuntimeConfigService } from '../config/runtime-config.service';

export const DEFAULT_SITE_SETTINGS: SiteSettingsResponse = {
  brandName: 'Shortener Ops',
  tagline: 'Secure link management for teams and workspaces.',
  supportEmail: 'support@example.com',
  supportUrl: 'https://example.com/help',
  contactText: 'Demo support is handled through the project owner.',
  logo: null,
  logoDark: null,
  favicon: null,
  loginBackground: null,
  landingHero: null,
  primaryColor: '#185A9D',
  secondaryColor: '#123047',
  accentColor: '#0F766E',
  footerDescription: 'A production-oriented URL shortener prototype with secure ownership and operational controls.',
  footerCopyright: 'Copyright 2026 Shortener Ops demo.',
  version: 0
};

@Injectable({ providedIn: 'root' })
export class SiteExperienceService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private readonly settingsSignal = signal<SiteSettingsResponse>(DEFAULT_SITE_SETTINGS);
  private loaded = false;

  readonly settings = this.settingsSignal.asReadonly();
  readonly brandName = computed(() => this.settingsSignal().brandName || DEFAULT_SITE_SETTINGS.brandName);

  loadSettings(): Observable<SiteSettingsResponse> {
    if (this.loaded) {
      return of(this.settingsSignal());
    }
    return this.http.get<SiteSettingsResponse>(this.config.apiUrl('/api/v1/site/settings')).pipe(
      tap((settings) => {
        this.settingsSignal.set({ ...DEFAULT_SITE_SETTINGS, ...settings });
        this.applyBrandTokens(this.settingsSignal());
        this.loaded = true;
      }),
      catchError(() => {
        this.settingsSignal.set(DEFAULT_SITE_SETTINGS);
        this.applyBrandTokens(DEFAULT_SITE_SETTINGS);
        this.loaded = true;
        return of(DEFAULT_SITE_SETTINGS);
      })
    );
  }

  page(pageKey: ContentPageKey): Observable<PublicContentPageResponse> {
    return this.http.get<PublicContentPageResponse>(this.config.apiUrl(`/api/v1/site/pages/${pageKey}`));
  }

  announcements(audience: AnnouncementAudience = 'PUBLIC'): Observable<AnnouncementResponse[]> {
    return this.http.get<AnnouncementResponse[]>(this.config.apiUrl('/api/v1/site/announcements'), {
      params: { audience }
    }).pipe(catchError(() => of([])));
  }

  adminSettings(): Observable<ResourceWithEtag<SiteSettingsResponse>> {
    return this.http.get<SiteSettingsResponse>(this.config.apiUrl('/api/v1/admin/site/settings'), { observe: 'response' }).pipe(
      map((response) => ({ body: response.body as SiteSettingsResponse, etag: response.headers.get('ETag') }))
    );
  }

  updateSettings(request: SiteSettingsUpdateRequest, etag: string): Observable<ResourceWithEtag<SiteSettingsResponse>> {
    return this.http.put<SiteSettingsResponse>(this.config.apiUrl('/api/v1/admin/site/settings'), request, {
      headers: new HttpHeaders({ 'If-Match': etag }),
      observe: 'response'
    }).pipe(map((response) => ({ body: response.body as SiteSettingsResponse, etag: response.headers.get('ETag') })));
  }

  adminPages(): Observable<AdminContentPageResponse[]> {
    return this.http.get<AdminContentPageResponse[]>(this.config.apiUrl('/api/v1/admin/site/content'));
  }

  updatePage(pageKey: ContentPageKey, request: ContentPageUpdateRequest, etag: string): Observable<ResourceWithEtag<AdminContentPageResponse>> {
    return this.http.put<AdminContentPageResponse>(this.config.apiUrl(`/api/v1/admin/site/content/${pageKey}`), request, {
      headers: new HttpHeaders({ 'If-Match': etag }),
      observe: 'response'
    }).pipe(map((response) => ({ body: response.body as AdminContentPageResponse, etag: response.headers.get('ETag') })));
  }

  adminAnnouncements(): Observable<AnnouncementResponse[]> {
    return this.http.get<AnnouncementResponse[]>(this.config.apiUrl('/api/v1/admin/site/announcements'));
  }

  createAnnouncement(request: AnnouncementRequest): Observable<ResourceWithEtag<AnnouncementResponse>> {
    return this.http.post<AnnouncementResponse>(this.config.apiUrl('/api/v1/admin/site/announcements'), request, { observe: 'response' })
      .pipe(map((response) => ({ body: response.body as AnnouncementResponse, etag: response.headers.get('ETag') })));
  }

  updateAnnouncement(id: string, request: AnnouncementRequest, etag: string): Observable<ResourceWithEtag<AnnouncementResponse>> {
    return this.http.put<AnnouncementResponse>(this.config.apiUrl(`/api/v1/admin/site/announcements/${id}`), request, {
      headers: new HttpHeaders({ 'If-Match': etag }),
      observe: 'response'
    }).pipe(map((response) => ({ body: response.body as AnnouncementResponse, etag: response.headers.get('ETag') })));
  }

  adminMedia(): Observable<MediaAssetResponse[]> {
    return this.http.get<MediaAssetResponse[]>(this.config.apiUrl('/api/v1/admin/site/media'));
  }

  registerMedia(request: MediaAssetRequest): Observable<ResourceWithEtag<MediaAssetResponse>> {
    return this.http.post<MediaAssetResponse>(this.config.apiUrl('/api/v1/admin/site/media'), request, { observe: 'response' })
      .pipe(map((response) => ({ body: response.body as MediaAssetResponse, etag: response.headers.get('ETag') })));
  }

  private applyBrandTokens(settings: SiteSettingsResponse): void {
    const root = document.documentElement;
    root.style.setProperty('--color-brand', settings.primaryColor);
    root.style.setProperty('--color-brand-hover', settings.secondaryColor);
    root.style.setProperty('--color-accent', settings.accentColor);
  }
}
