import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// -----------------------------------------------------------------------
// 1. 설정 (Configuration)
// -----------------------------------------------------------------------
const BASE_URL = 'http://localhost:8080'; // 서버 주소
const TARGET_POST_ID = 33; // 테스트할 게시글 ID (DB에 실제 존재하는 ID여야 함)
// 수정된 엔드포인트: 컨트롤러의 @RequestMapping("/api/meow/boast-cat")과 @GetMapping("/{boastCatPostId}")를 반영
const ENDPOINT = `${BASE_URL}/api/meow/boast-cat/${TARGET_POST_ID}`;

// 성공한 요청 수를 세기 위한 커스텀 메트릭
const successfulRequests = new Counter('successful_requests');

export const options = {
  // 부하 테스트 시나리오 설정
  scenarios: {
    view_count_spike: {
      executor: 'ramping-vus', // 가상 유저(VU)를 점진적으로 늘림
      startVUs: 0,
      stages: [
        { duration: '5s', target: 50 },  // 5초 동안 50명까지 증가 (Warming up)
        { duration: '10s', target: 100 }, // 10초 동안 100명 유지 (Peak Load)
        { duration: '5s', target: 0 },   // 5초 동안 0명으로 감소 (Cool down)
      ],
      gracefulRampDown: '0s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // 실패율이 1% 미만이어야 함
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내에 처리되어야 함
  },
};

// -----------------------------------------------------------------------
// 2. 초기화 (Setup) - 테스트 시작 전 1회 실행
// -----------------------------------------------------------------------
export function setup() {
  // 테스트 시작 전 현재 조회수를 가져옵니다.
  const res = http.get(ENDPOINT);
  
  check(res, {
    'Setup: Get initial view count status is 200': (r) => r.status === 200,
  });

  try {
    const body = JSON.parse(res.body);
    // ApiResponse 구조: { status, message, data: { ... } }
    // 실제 데이터는 body.data 안에 있을 것으로 추정됨
    const data = body.data || {};
    const initialViewCount = data.view || 0;  // 실제 API 필드명은 'view' 
    console.log(`[Setup] Initial View Count: ${initialViewCount}`);
    return { initialViewCount };
  } catch (e) {
    console.error('[Setup] Failed to parse initial view count', e);
    return { initialViewCount: 0 };
  }
}

// -----------------------------------------------------------------------
// 3. 메인 로직 (Default) - 각 VU가 반복 실행
// -----------------------------------------------------------------------
export default function () {
  const res = http.get(ENDPOINT);

  const isSuccess = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  if (isSuccess) {
    successfulRequests.add(1); // 성공한 요청 카운트 증가
  }

  sleep(0.1); // 너무 빠른 요청 방지 (필요 시 조절)
}

// -----------------------------------------------------------------------
// 4. 정리 및 검증 (Teardown) - 테스트 종료 후 1회 실행
// -----------------------------------------------------------------------
export function teardown(data) {
  // Write-Back 패턴(Redis 등)을 사용하는 경우 DB 반영까지 시간이 걸릴 수 있으므로 잠시 대기
  // sleep(2); 

  const res = http.get(ENDPOINT);
  let finalViewCount = 0;

  try {
      const body = JSON.parse(res.body);
      const resData = body.data || {};
      finalViewCount = resData.view || 0;  // 실제 API 필드명은 'view'
  } catch(e) {
      console.error('[Teardown] Failed to parse final view count', e);
  }
  
  // K6가 집계한 성공 요청 수 (각 노드에서 실행된 경우 정확하지 않을 수 있어 metrics API 사용 권장되나, 단일 실행에선 근사치로 사용 가능)
  // 여기서는 setup에서 넘겨받은 데이터와 최종 데이터를 비교합니다.
  
  const expectedIncrease = finalViewCount - data.initialViewCount;

  console.log('---------------------------------------------------');
  console.log(`[Result] Initial View Count : ${data.initialViewCount}`);
  console.log(`[Result] Final View Count   : ${finalViewCount}`);
  console.log(`[Result] Total Increased    : ${expectedIncrease}`);
  console.log('---------------------------------------------------');

  // 주의: K6의 successfulRequests 카운터 값은 teardown에서 직접 접근이 어렵습니다.
  // 따라서 콘솔에 찍힌 'successful_requests' 값과 위 'Total Increased' 값을 비교해야 합니다.
  
  if (expectedIncrease === 0) {
      console.warn("조회수가 정상적으로 카운트되지 않음.");
  }
}
