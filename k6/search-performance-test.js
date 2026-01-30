import http from 'k6/http';
import { check } from 'k6';

export const options = {
  // 30초 동안 50명의 유저가 지속적으로 검색 요청을 보냄
  scenarios: {
    search_tps_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 50 },  // 5초간 50명까지 증가
        { duration: '20s', target: 50 }, // 20초간 유지 (TPS 측정)
        { duration: '5s', target: 0 },   // 5초간 종료
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'], // 에러율 1% 미만
    http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초 이내 처리
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {

  const payload = JSON.stringify({
    title: "고양이", // 실제 DB에 존재할 법한 검색어
    // contents: "내용" // 필요시 추가
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // POST /api/meow/search 엔드포인트 호출

  const res = http.post(`${BASE_URL}/api/meow/search?page=0&size=10`, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
