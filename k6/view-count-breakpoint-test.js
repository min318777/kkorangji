import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const POST_ID = 33;  // 테스트할 게시글 ID

const VIEW_COUNT_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}/view`;
const GET_POST_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}`;

const successCount = new Counter('success_count');

/**
 * 브레이크포인트 테스트
 * - 시스템이 깨지는 지점(Breaking Point)을 찾는 테스트
 * - 초당 요청 수를 점진적으로 증가시켜 한계점 확인
 */
export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-arrival-rate',
      startRate: 10,           // 시작: 초당 10 요청
      timeUnit: '1s',
      preAllocatedVUs: 100,    // 미리 할당할 VU
      maxVUs: 2000,            // 최대 VU
      stages: [
        { duration: '30s', target: 50 },    // 30초 동안 초당 50 요청까지
        { duration: '30s', target: 100 },   // 30초 동안 초당 100 요청까지
        { duration: '30s', target: 200 },   // 30초 동안 초당 200 요청까지
        { duration: '30s', target: 300 },   // 30초 동안 초당 300 요청까지
        { duration: '30s', target: 500 },   // 30초 동안 초당 500 요청까지
        { duration: '30s', target: 700 },   // 30초 동안 초당 700 요청까지
        { duration: '30s', target: 1000 },  // 30초 동안 초당 1000 요청까지
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],       // 실패율 10% 미만
    http_req_duration: ['p(95)<3000'],    // 95% 요청이 3초 이내
  },
};

export function setup() {
  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const initialView = body.data?.view || 0;

  console.log(`\n${'='.repeat(50)}`);
  console.log(`🎯 브레이크포인트 테스트 시작`);
  console.log(`${'='.repeat(50)}`);
  console.log(`📊 초기 조회수: ${initialView}`);
  console.log(`\n📈 초당 요청 수 단계 (RPS):`);
  console.log(`  0~30초:    10 → 50 RPS`);
  console.log(`  30~60초:   50 → 100 RPS`);
  console.log(`  60~90초:   100 → 200 RPS`);
  console.log(`  90~120초:  200 → 300 RPS`);
  console.log(`  120~150초: 300 → 500 RPS`);
  console.log(`  150~180초: 500 → 700 RPS`);
  console.log(`  180~210초: 700 → 1000 RPS`);
  console.log(`${'='.repeat(50)}\n`);

  return { initialView, startTime: Date.now() };
}

export default function () {
  const res = http.post(VIEW_COUNT_API);

  if (check(res, { 'status 200': (r) => r.status === 200 })) {
    successCount.add(1);
  }
}

export function teardown(data) {
  sleep(2);

  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const finalView = body.data?.view || 0;

  const increase = finalView - data.initialView;
  const totalDuration = ((Date.now() - data.startTime) / 1000).toFixed(1);

  console.log(`\n${'='.repeat(50)}`);
  console.log(`📊 브레이크포인트 테스트 결과`);
  console.log(`${'='.repeat(50)}`);
  console.log(`⏱️  총 테스트 시간: ${totalDuration}초`);
  console.log(`📈 초기 조회수: ${data.initialView}`);
  console.log(`📈 최종 조회수: ${finalView}`);
  console.log(`📈 증가한 조회수: ${increase}`);
  console.log(`${'='.repeat(50)}`);
  console.log(`\n🔍 브레이크포인트 분석:`);
  console.log(`   http_req_duration p(95)가 급격히 증가하는 구간 = 병목 시작점`);
  console.log(`   http_req_failed가 증가하기 시작하는 구간 = 브레이크포인트`);
  console.log(`\n🔍 정합성 검증:`);
  console.log(`   success_count vs 증가한 조회수 비교`);
  console.log(`   차이가 발생하면 해당 부하에서 동시성 문제 발생`);
  console.log(`${'='.repeat(50)}\n`);
}
