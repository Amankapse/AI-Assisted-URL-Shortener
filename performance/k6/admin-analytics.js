import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 2),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<750'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ADMIN_ACCESS_TOKEN;

export default function () {
  const response = http.get(`${baseUrl}/api/v1/admin/analytics/overview`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(response, {
    'admin analytics ok or limited': (r) => r.status === 200 || r.status === 429,
  });
}
