import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<200'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const prefix = __ENV.SHORT_CODE_PREFIX || 'miss';

export default function () {
  const code = `${prefix}${__VU}${__ITER}`;
  const response = http.get(`${baseUrl}/r/${code}`, { redirects: 0 });
  check(response, {
    'miss is safe 404 or redirect': (r) => r.status === 404 || r.status === 302,
  });
}
