import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ================= 설정 영역 =================
const BASE_URL = 'http://localhost:8080';
const TARGET_POST_ID = 1; // 테스트할 게시글 ID (DB에 미리 생성해두세요!)

export const options = {
  scenarios: {
    like_storm: {
      executor: 'ramping-vus', // 점진적으로 유저 증가
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },  // 10초 동안 50명까지 증가 (Warming up)
        { duration: '30s', target: 100 }, // 30초 동안 100명 유지 (Peak Load)
        { duration: '10s', target: 0 },   // 10초 동안 감소
      ],
      gracefulStop: '5s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내여야 함 (실패할 것으로 예상)
    http_req_failed: ['rate<0.01'],   // 에러율 1% 미만
  },
};

export default function () {
  // 1. 랜덤 유저 생성 (매번 새로운 유저로 좋아요 시도)
  const username = `user_${randomString(8)}`;
  const password = 'password123!';
  const email = `${username}@test.com`;
  const nickname = `nick_${randomString(5)}`;

  // 2. 회원가입
  const signupPayload = JSON.stringify({
    username: username,
    password: password,
    email: email,
    nickname: nickname,
  });

  const signupHeaders = { 'Content-Type': 'application/json' };
  const signupRes = http.post(`${BASE_URL}/api/users/signup`, signupPayload, { headers: signupHeaders });

  check(signupRes, {
    'signup successful': (r) => r.status === 200 || r.status === 201,
  });

  // 3. 로그인 (토큰 발급)
  const loginPayload = JSON.stringify({
    username: username,
    password: password,
  });

  const loginRes = http.post(`${BASE_URL}/api/users/login`, loginPayload, { headers: signupHeaders });
  
  const isLoginSuccess = check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  if (!isLoginSuccess) {
    console.error(`Login failed for ${username}`);
    return;
  }

  // 응답에서 Access Token 추출 (구조에 맞게 수정 필요)
  // 예: response body가 { "accessToken": "..." } 형태라고 가정
  const accessToken = loginRes.json('accessToken'); 
  // 만약 헤더에 있다면: loginRes.headers['Authorization'];

  // 4. 좋아요 요청 (핵심 테스트 구간)
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`, // Bearer 토큰
  };

  const likeRes = http.post(`${BASE_URL}/api/posts/${TARGET_POST_ID}/likes`, null, { headers: authHeaders });

  check(likeRes, {
    'like successful': (r) => r.status === 200,
  });

  sleep(1);
}