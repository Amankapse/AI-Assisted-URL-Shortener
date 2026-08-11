import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const outputPath = resolve('dist/frontend/browser/app-config.json');
const config = {
  apiBaseUrl: optionalSameOriginUrl('FRONTEND_API_BASE_URL'),
  publicShortUrlBase: requiredUrl('FRONTEND_PUBLIC_SHORT_URL_BASE'),
  environment: process.env.FRONTEND_ENVIRONMENT || 'production'
};

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, `${JSON.stringify(config, null, 2)}\n`, 'utf8');
console.log(`Wrote public runtime config to ${outputPath}`);

function requiredUrl(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} is required`);
  }
  const url = new URL(value);
  if (url.protocol !== 'https:' && url.protocol !== 'http:') {
    throw new Error(`${name} must be an HTTP or HTTPS URL`);
  }
  return url.toString().replace(/\/$/, '');
}

function optionalSameOriginUrl(name) {
  const value = process.env[name] ?? '';
  if (value === '') {
    return '';
  }
  const url = new URL(value);
  if (url.protocol !== 'https:' && url.protocol !== 'http:') {
    throw new Error(`${name} must be empty or an HTTP/HTTPS URL`);
  }
  return url.toString().replace(/\/$/, '');
}
