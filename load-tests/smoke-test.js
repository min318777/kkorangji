import http from 'k6/http';
import { check, sleep } from 'k6';

// 스모크 테스트: 기본 기능이 정상 작동하는지 확인 (부하 없음)
export const options = {
  vus: 1,           // 가상 유저 1명
  duration: '30s',  // 30초 동안
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 1. 게시글 목록 조회
  let response = http.get(`${BASE_URL}/api/meow/boast-cat?page=0&size=10`);
  check(response, {
    '게시글 목록 조회 성공': (r) => r.status === 200,
  });

  sleep(1);

  // 2. 단일 게시글 조회
  response = http.get(`${BASE_URL}/api/meow/boast-cat/1`);
  check(response, {
    '게시글 조회 성공': (r) => r.status === 200,
  });

  sleep(1);

  // 3. 로그인
  const loginPayload = JSON.stringify({
    loginId: 'testuser',
    password: 'password123',
  });

  response = http.post(
    `${BASE_URL}/api/users/login`,
    loginPayload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginSuccess = check(response, {
    '로그인 성공': (r) => r.status === 200,
  });

  if (loginSuccess) {
    const token = JSON.parse(response.body).accessToken;

    // 4. 인증이 필요한 요청 테스트
    response = http.get(`${BASE_URL}/api/notice`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });

    check(response, {
      '알림 조회 성공': (r) => r.status === 200,
    });
  }

  sleep(2);
}
