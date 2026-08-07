import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<50'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const shortCode = __ENV.SHORT_CODE || 'cachehit';

export function setup() {
  http.get(`${baseUrl}/r/${shortCode}`, { redirects: 0 });
}

export default function () {
  const response = http.get(`${baseUrl}/r/${shortCode}`, { redirects: 0 });
  check(response, {
    'redirect returned': (r) => r.status === 302,
  });
}
