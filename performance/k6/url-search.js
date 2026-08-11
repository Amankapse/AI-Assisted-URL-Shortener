import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;
const query = encodeURIComponent(__ENV.SEARCH_QUERY || 'example');

export default function () {
  const res = http.get(`${baseUrl}/api/v1/urls?q=${query}&state=ACTIVE&page=0&size=20&sort=createdAt,desc`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(res, {
    'search status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
