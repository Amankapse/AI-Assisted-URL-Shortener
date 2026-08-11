import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppConfig } from './app-config.model';

@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private readonly http = inject(HttpClient);
  private readonly currentConfig = signal<AppConfig | null>(null);

  readonly config = this.currentConfig.asReadonly();

  async load(): Promise<void> {
    const config = await firstValueFrom(this.http.get<AppConfig>('/app-config.json'));
    this.currentConfig.set(this.validate(config));
  }

  requireConfig(): AppConfig {
    const config = this.currentConfig();
    if (!config) {
      throw new Error('Application configuration has not been loaded.');
    }
    return config;
  }

  apiUrl(path: string): string {
    const base = this.requireConfig().apiBaseUrl.replace(/\/+$/, '');
    const cleanPath = path.startsWith('/') ? path : `/${path}`;
    return `${base}${cleanPath}`;
  }

  private validate(config: Partial<AppConfig>): AppConfig {
    if (config.apiBaseUrl === undefined || config.apiBaseUrl === null || !isApiBaseUrl(config.apiBaseUrl)) {
      throw new Error('Runtime configuration requires a valid apiBaseUrl.');
    }
    if (!config.publicShortUrlBase || !isHttpUrl(config.publicShortUrlBase)) {
      throw new Error('Runtime configuration requires a valid publicShortUrlBase.');
    }
    return {
      apiBaseUrl: config.apiBaseUrl,
      publicShortUrlBase: config.publicShortUrlBase,
      environment: config.environment ?? 'unknown'
    };
  }
}

function isHttpUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

function isApiBaseUrl(value: string): boolean {
  return value === '' || isHttpUrl(value);
}
