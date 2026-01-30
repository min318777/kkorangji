import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const POST_ID = 33;

// 조회수 증가 API (POST)
const VIEW_COUNT_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}/view`;
// 게시글 조회 API (GET) - 조회수 확인용
const GET_POST_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}`;

// 성공한 요청 수 카운터
const successCount = new Counter('success_count');

export const options = {
  // 100명이 동시에 30초간 요청
  vus: 100,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],      // 실패율 1% 미만
    http_req_duration: ['p(95)<500'],    // 95% 요청이 500ms 이내
  },
};

// 초기 조회수 확인
export function setup() {
  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const initialView = body.data?.view || 0;

  console.log(`\n========== 테스트 시작 ==========`);
  console.log(`초기 조회수: ${initialView}`);
  console.log(`조회수 증가 API: POST ${VIEW_COUNT_API}`);
  console.log(`================================\n`);

  return { initialView };
}

export default function () {
  const res = http.post(VIEW_COUNT_API);

  if (check(res, { 'status 200': (r) => r.status === 200 })) {
    successCount.add(1);
  }
}

export function teardown(data) {
  sleep(1);  // DB 반영 대기

  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const finalView = body.data?.view || 0;

  const increase = finalView - data.initialView;

  console.log(`\n========== 테스트 결과 ==========`);
  console.log(`초기 조회수: ${data.initialView}`);
  console.log(`최종 조회수: ${finalView}`);
  console.log(`증가한 조회수: ${increase}`);
  console.log(`================================`);
  console.log(`\n 동시성 검증 방법:`);
  console.log(`위 K6 결과에서 'success_count' 값과 '증가한 조회수'를 비교하세요.`);
  console.log(`- 같으면 동시성 문제 없음`);
  console.log(`- 다르면 동시성 문제 있음 (조회수 유실)\n`);
}