export interface AppConfig {
  apiBaseUrl: string;
  publicShortUrlBase: string;
  environment: 'local' | 'development' | 'test' | 'staging' | 'production' | string;
}
