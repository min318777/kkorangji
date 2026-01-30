import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = 'http://localhost:8080';
const POST_ID = 33;

// 조회수 증가 API (POST)
const VIEW_COUNT_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}/view`;
// 게시글 조회 API (GET) - 조회수 확인용
const GET_POST_API = `${BASE_URL}/api/meow/boast-cat/${POST_ID}`;

// 커스텀 메트릭
const successCount = new Counter('success_count');
const failCount = new Counter('fail_count');
const responseTime = new Trend('view_api_response_time');

/**
 * 스트레스 테스트 시나리오
 * - 점진적으로 사용자 수를 늘려가며 시스템 한계점 확인
 * - 각 단계에서 조회수 정합성 검증
 */
export const options = {
  stages: [
    // 1단계: 워밍업 (10명 → 50명, 1분)
    { duration: '1m', target: 50 },

    // 2단계: 중간 부하 (50명 → 100명, 1분)
    { duration: '1m', target: 100 },

    // 3단계: 고부하 (100명 → 200명, 1분)
    { duration: '1m', target: 200 },

    // 4단계: 극한 부하 (200명 → 500명, 1분)
    { duration: '1m', target: 500 },

    // 5단계: 피크 부하 (500명 → 1000명, 1분)
    { duration: '1m', target: 1000 },

    // 6단계: 쿨다운 (1000명 → 0명, 30초)
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],       // 실패율 5% 미만
    http_req_duration: ['p(95)<2000'],    // 95% 요청이 2초 이내
  },
};

// 테스트 시작 전 초기 조회수 확인
export function setup() {
  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const initialView = body.data?.view || 0;

  console.log(`\n${'='.repeat(50)}`);
  console.log(`🚀 스트레스 테스트 시작`);
  console.log(`${'='.repeat(50)}`);
  console.log(`📊 초기 조회수: ${initialView}`);
  console.log(`🎯 테스트 API: POST ${VIEW_COUNT_API}`);
  console.log(`\n📈 부하 단계:`);
  console.log(`  1단계 (0~1분):   10명 → 50명`);
  console.log(`  2단계 (1~2분):   50명 → 100명`);
  console.log(`  3단계 (2~3분):   100명 → 200명`);
  console.log(`  4단계 (3~4분):   200명 → 500명`);
  console.log(`  5단계 (4~5분):   500명 → 1000명`);
  console.log(`  6단계 (5~5.5분): 쿨다운`);
  console.log(`${'='.repeat(50)}\n`);

  return { initialView, startTime: Date.now() };
}

// 메인 테스트 - 조회수 증가 API 호출
export default function () {
  const startTime = Date.now();
  const res = http.post(VIEW_COUNT_API);
  const duration = Date.now() - startTime;

  // 응답 시간 기록
  responseTime.add(duration);

  const success = check(res, {
    'status 200': (r) => r.status === 200,
  });

  if (success) {
    successCount.add(1);
  } else {
    failCount.add(1);
    // 실패 시 로그 출력 (디버깅용)
    if (res.status !== 200) {
      console.log(`❌ 실패: status=${res.status}, body=${res.body}`);
    }
  }
}

// 테스트 종료 후 결과 분석
export function teardown(data) {
  sleep(2);  // DB 반영 대기

  const res = http.get(GET_POST_API);
  const body = JSON.parse(res.body);
  const finalView = body.data?.view || 0;

  const increase = finalView - data.initialView;
  const totalDuration = ((Date.now() - data.startTime) / 1000).toFixed(1);

  console.log(`\n${'='.repeat(50)}`);
  console.log(`📊 스트레스 테스트 결과`);
  console.log(`${'='.repeat(50)}`);
  console.log(`⏱️  총 테스트 시간: ${totalDuration}초`);
  console.log(`📈 초기 조회수: ${data.initialView}`);
  console.log(`📈 최종 조회수: ${finalView}`);
  console.log(`📈 증가한 조회수: ${increase}`);
  console.log(`${'='.repeat(50)}`);
  console.log(`\n🔍 정합성 검증 방법:`);
  console.log(`   K6 결과의 'success_count' 값과 '증가한 조회수'를 비교하세요.`);
  console.log(`   ✅ 같으면: 동시성 문제 없음 (정합성 유지)`);
  console.log(`   ❌ 다르면: 동시성 문제 발생 (조회수 유실)`);
  console.log(`\n💡 성능 병목 확인:`);
  console.log(`   - http_req_duration p(95) > 1초: DB 병목 가능성`);
  console.log(`   - http_req_failed > 1%: 서버 과부하 상태`);
  console.log(`${'='.repeat(50)}\n`);
}
