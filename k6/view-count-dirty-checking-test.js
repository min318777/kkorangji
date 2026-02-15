/**
 * 조회수 테스트 - v1 더티체킹 방식
 * ⚠️ Lost Update 발생 (동시성 이슈 있음)
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
const VIEW_COUNT_API = `${BASE_URL}/api/meow/boast-cat/v1/${POST_ID}/view`;

export default function() {
  const response = http.post(VIEW_COUNT_API, null, { timeout: '30s' });
  const success = check(response, { '200 OK': (r) => r.status === 200 });
  if (success) successfulRequests.add(1);
  sleep(0.1);
}

export function teardown() {
  console.log('✅ v1 더티체킹 테스트 완료');
}
