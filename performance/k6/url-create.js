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

export default function () {
  const body = JSON.stringify({
    originalUrl: `https://example.com/load/${__VU}/${__ITER}`,
    customAlias: `k6_${__VU}_${__ITER}_${Date.now()}`,
  });
  const response = http.post(`${baseUrl}/api/v1/urls`, body, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  check(response, {
    'created': (r) => r.status === 201,
  });
}
