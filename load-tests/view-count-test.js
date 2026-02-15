import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { textSummary } from 'https://raw.githubusercontent.com/grafana/k6-reporter/main/dist/bundle.js';

let teardownMessage = '';

// 커스텀 메트릭
const viewCountErrors = new Counter('view_count_errors');

// 조회수 테스트 설정
export const options = {
  scenarios: {
    // 시나리오 1: 동시 조회 폭증 (조회수 손실 테스트)
    concurrent_views: {
      executor: 'constant-arrival-rate',
      rate: 100,        // 초당 100개 요청
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300'],
    http_req_failed: ['rate<0.05'],
  },
};

const POST_ID = 1;  // 실제 존재하는 게시글 ID로 변경
const BASE_URL = 'http://localhost:8080';

export default function () {
  // 게시글 조회 (조회수 증가)
  const response = http.get(`${BASE_URL}/api/meow/boast-cat/${POST_ID}`);

  const success = check(response, {
    '조회 성공': (r) => r.status === 200,
    '응답 시간 300ms 이하': (r) => r.timings.duration < 300,
  });

  if (!success) {
    viewCountErrors.add(1);
  }

  sleep(0.1);
}

export function teardown(data) {
  // 테스트 종료 후 실제 조회수 확인
  const response = http.get(`${BASE_URL}/api/meow/boast-cat/${POST_ID}`);

  if (response.status === 200) {
    const post = JSON.parse(response.body);
    console.log(`
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    📈 조회수 정확성 검증
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    최종 조회수: ${post.view}

    ⚠️  예상 조회수와 비교해보세요!
    동시성 이슈가 있다면 실제 요청 수보다 낮게 나올 수 있습니다.
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    `);
  }
}
