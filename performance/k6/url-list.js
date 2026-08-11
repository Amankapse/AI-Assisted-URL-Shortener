import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;

export default function () {
  const res = http.get(`${baseUrl}/api/v1/urls?page=0&size=${__ENV.SIZE || 20}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(res, {
    'list status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
