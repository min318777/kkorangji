import http from 'k6/http';
import { check } from 'k6';

// 테스트 설정
export const options = {
  // 30초 동안 VUs(가상 유저)를 50명까지 늘려서 최대 TPS 측정
  scenarios: {
    tps_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 50 },  // 5초 동안 50명까지 증가
        { duration: '20s', target: 50 }, // 20초 동안 50명 유지 (안정적인 TPS 측정 구간)
        { duration: '5s', target: 0 },   // 5초 동안 종료
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // 에러율 1% 미만이어야 함
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 최근 게시물 조회 API 호출
  const res = http.get(`${BASE_URL}/api/meow/boast-cat/recent`);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}