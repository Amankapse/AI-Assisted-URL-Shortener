import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 2),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const email = __ENV.LOGIN_EMAIL || 'load@example.com';
const password = __ENV.LOGIN_PASSWORD || 'correct-horse-password';

export default function () {
  const response = http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({ email, password }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(response, {
    'login ok or rate limited': (r) => r.status === 200 || r.status === 429,
  });
}
