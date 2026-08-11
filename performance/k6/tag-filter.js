import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || '30s',
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.ACCESS_TOKEN;
const tag = encodeURIComponent(__ENV.TAG || 'paid');

export default function () {
  const res = http.get(`${baseUrl}/api/v1/urls?tag=${tag}&page=0&size=20`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(res, {
    'tag filter status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
