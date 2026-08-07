import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 3),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<300'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;
const urlId = __ENV.URL_ID;

export default function () {
  const response = http.get(`${baseUrl}/api/v1/urls/${urlId}/analytics`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(response, {
    'analytics ok': (r) => r.status === 200,
  });
}
