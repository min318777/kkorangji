import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// 커스텀 메트릭
const likeErrors = new Counter('like_errors');
const duplicateLikes = new Counter('duplicate_likes');

// 테스트 설정
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 10명의 유저로 30초간 증가
    { duration: '1m', target: 50 },   // 50명의 유저로 1분간 증가
    { duration: '30s', target: 100 }, // 100명의 유저로 30초간 증가
    { duration: '1m', target: 100 },  // 100명 유지
    { duration: '30s', target: 0 },   // 점진적으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이하
    http_req_failed: ['rate<0.1'],     // 실패율 10% 미만
  },
};

// 테스트할 게시글 ID (실제 존재하는 ID로 변경 필요)
const POST_ID = 1;
const BASE_URL = 'http://localhost:8080';

// 로그인하여 JWT 토큰 획득
function login() {
  const loginPayload = JSON.stringify({
    loginId: 'testuser',  // 실제 테스트 계정으로 변경
    password: 'password123'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = http.post(`${BASE_URL}/api/users/login`, loginPayload, params);

  if (response.status === 200) {
    const jsonResponse = JSON.parse(response.body);
    return jsonResponse.accessToken;
  }

  return null;
}

export default function () {
  // 각 가상 유저가 로그인
  const token = login();

  if (!token) {
    console.error('로그인 실패');
    return;
  }

  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  // 좋아요 토글 (동시에 여러 번 클릭하는 상황 시뮬레이션)
  for (let i = 0; i < 5; i++) {
    const response = http.post(
      `${BASE_URL}/api/meow/boast-cat/${POST_ID}/like`,
      null,
      params
    );

    const success = check(response, {
      '좋아요 요청 성공': (r) => r.status === 200,
      '응답 시간 500ms 이하': (r) => r.timings.duration < 500,
    });

    if (!success) {
      likeErrors.add(1);
      console.log(`좋아요 실패: Status ${response.status}, Body: ${response.body}`);
    }

    // 매우 짧은 대기 (동시성 테스트를 위해)
    sleep(0.1);
  }

  // 잠시 대기 후 다시 시도
  sleep(1);
}

export function handleSummary(data) {
  return {
    'summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  const enableColors = options.enableColors || false;

  let summary = `
${indent}✓ 테스트 완료
${indent}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

${indent}📊 요청 통계:
${indent}  총 요청 수: ${data.metrics.http_reqs.values.count}
${indent}  실패한 요청: ${data.metrics.http_req_failed.values.passes || 0}
${indent}  성공률: ${((1 - (data.metrics.http_req_failed.values.rate || 0)) * 100).toFixed(2)}%

${indent}⏱️  응답 시간:
${indent}  평균: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
${indent}  최소: ${data.metrics.http_req_duration.values.min.toFixed(2)}ms
${indent}  최대: ${data.metrics.http_req_duration.values.max.toFixed(2)}ms
${indent}  p95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
${indent}  p99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms

${indent}🔄 동시 접속:
${indent}  최대 VUs: ${data.metrics.vus_max.values.max}
  `;

  return summary;
}
