/**
 * 조회수 테스트 - v2 원자적 쿼리 방식
 * UPDATE view = view + 1 (DB 레벨 원자성)
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const successfulRequests = new Counter('successful_requests');

export const options = {
  stages: [
    { duration: '1m', target: 1000 },
  ],
};

const POST_ID = 147516;
const BASE_URL = 'http://localhost:8080';
const VIEW_COUNT_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}/view`;

export default function() {
  const response = http.post(VIEW_COUNT_API, null, { timeout: '30s' });
  const success = check(response, { '200 OK': (r) => r.status === 200 });
  if (success) successfulRequests.add(1);
  sleep(0.1);
}

export function teardown() {
  console.log('✅ v2 원자적쿼리 테스트 완료');
}
