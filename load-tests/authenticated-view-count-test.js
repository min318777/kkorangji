import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// 커스텀 메트릭
const loginErrors = new Counter('login_errors');
const viewCountErrors = new Counter('view_count_errors');

// 테스트 설정
export const options = {
  stages: [
    { duration: '10s', target: 10 },  // 10초간 10명의 사용자로 증가
    { duration: '20s', target: 10 },  // 20초간 10명의 사용자 유지
    { duration: '10s', target: 0 },   // 10초간 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95%의 요청이 500ms 이하
    http_req_failed: ['rate<0.1'],     // 실패율 10% 이하
  },
};

const BASE_URL = 'http://localhost:8080';

// 테스트할 게시글 ID (실제 존재하는 게시글 ID로 변경 필요)
const POST_ID = 32;

// 테스트 사용자 정보 (실제 DB에 존재하는 사용자로 변경 필요)
const TEST_USER = {
  loginId: 'min3187',
  password: '1111'
};

/**
 * 로그인하여 Access Token을 받아옵니다
 * @returns {string|null} Access Token 또는 실패 시 null
 */
function login() {
  const loginPayload = JSON.stringify({
    loginId: TEST_USER.loginId,
    password: TEST_USER.password,
    rememberMe: false
  });

  const loginParams = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const loginResponse = http.post(
    `${BASE_URL}/api/users/login`,
    loginPayload,
    loginParams
  );

  const loginSuccess = check(loginResponse, {
    '로그인 성공 ': (r) => r.status === 200,
    '액세스 토큰 존재': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.accessToken !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!loginSuccess) {
    loginErrors.add(1);
    console.error(`로그인 실패: ${loginResponse.status} - ${loginResponse.body}`);
    return null;
  }

  // JSON 응답에서 accessToken 추출
  const responseBody = JSON.parse(loginResponse.body);
  return responseBody.accessToken;
}

/**
 * 메인 테스트 함수
 */
export default function () {
  // 1. 로그인하여 토큰 받기
  const accessToken = login();

  if (!accessToken) {
    console.error('액세스 토큰을 받지 못했습니다. 테스트를 건너뜁니다.');
    sleep(1);
    return;
  }

  // 2. 인증된 상태에서 게시글 조회 전 상태 확인
  const beforeViewResponse = http.get(
    `${BASE_URL}/api/meow/boast-cat/${POST_ID}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    }
  );

  let beforeViewCount = 0;
  if (beforeViewResponse.status === 200) {
    const beforePost = JSON.parse(beforeViewResponse.body);
    beforeViewCount = beforePost.data ? beforePost.data.view : beforePost.view;
    console.log(`조회 전 조회수: ${beforeViewCount}`);
  }

  // 짧은 대기
  sleep(0.5);

  // 3. 같은 게시글 다시 조회 (조회수가 증가해야 함)
  const afterViewResponse = http.get(
    `${BASE_URL}/api/meow/boast-cat/${POST_ID}`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    }
  );

  const viewCheckSuccess = check(afterViewResponse, {
    '게시글 조회 성공': (r) => r.status === 200,
    '응답 시간 500ms 이하': (r) => r.timings.duration < 500,
    '조회수 증가 확인': (r) => {
      if (r.status !== 200) return false;

      try {
        const afterPost = JSON.parse(r.body);
        const afterViewCount = afterPost.data ? afterPost.data.view : afterPost.view;
        console.log(`조회 후 조회수: ${afterViewCount}, 증가량: ${afterViewCount - beforeViewCount}`);

        // 조회수가 증가했는지 확인 (최소 1 이상 증가)
        return afterViewCount > beforeViewCount;
      } catch (e) {
        console.error('응답 파싱 실패:', e);
        return false;
      }
    },
  });

  if (!viewCheckSuccess) {
    viewCountErrors.add(1);
  }

  sleep(1);
}

/**
 * 테스트 시작 전 실행 (선택사항)
 */
export function setup() {
  console.log(`
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🧪 인증된 사용자 조회수 테스트 시작
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  테스트 대상 게시글 ID: ${POST_ID}
  테스트 사용자: ${TEST_USER.loginId}

  ⚠️  주의사항:
  1. 테스트 사용자가 DB에 존재해야 합니다
  2. 게시글 ID가 실제로 존재해야 합니다
  3. MySQL과 Redis가 실행 중이어야 합니다
  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  `);

  // 초기 조회수 확인
  const initialResponse = http.get(`${BASE_URL}/api/meow/boast-cat/${POST_ID}`);

  if (initialResponse.status === 200) {
    const initialPost = JSON.parse(initialResponse.body);
    const initialViewCount = initialPost.data ? initialPost.data.view : initialPost.view;
    console.log(`초기 조회수: ${initialViewCount}`);

    return { initialViewCount };
  } else {
    console.error(`게시글 조회 실패: ${initialResponse.status}`);
    console.error(`응답: ${initialResponse.body}`);
    return null;
  }
}

/**
 * 테스트 종료 후 실행
 */
export function teardown(data) {
  if (!data) {
    console.error('Setup 단계에서 데이터를 가져오지 못했습니다.');
    return;
  }

  // 최종 조회수 확인
  const finalResponse = http.get(`${BASE_URL}/api/meow/boast-cat/${POST_ID}`);

  if (finalResponse.status === 200) {
    const finalPost = JSON.parse(finalResponse.body);
    const finalViewCount = finalPost.data ? finalPost.data.view : finalPost.view;
    const increase = finalViewCount - data.initialViewCount;

    console.log(`
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    📊 조회수 테스트 결과
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    초기 조회수: ${data.initialViewCount}
    최종 조회수: ${finalViewCount}
    증가량: ${increase}

    ${increase > 0 ? '조회수가 정상적으로 증가했습니다!' : '조회수가 증가하지 않았습니다!'}

    💡 참고:
    - @Transactional이 적용되어 있어 조회수가 DB에 반영됩니다
    - increaseView() 메서드가 호출될 때마다 +1씩 증가합니다
    - 동시성 이슈가 있다면 실제 요청 수보다 적게 증가할 수 있습니다
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    `);
  } else {
    console.error(`최종 조회수 확인 실패: ${finalResponse.status}`);
  }
}
