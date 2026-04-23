/**
 * 최근 게시글 - 캐시 있는 버전 부하 테스트
 *
 * 실행 전 준비:
 *   BoastCatPostService.java 에서 @Cacheable 주석 해제 후 재빌드
 *   @Cacheable(cacheNames = "post:boast:recent")
 *
 * 실행:
 *   k6 run k6/recent-post-with-cache-test.js
 *
 * 확인 지표:
 *   http_req_duration: avg, p(95), p(99) ← hey의 P95/P99와 동일
 *   http_reqs: 초당 요청 수 (TPS)
 */
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 100,          // 동시 100명 (-c 100)
    iterations: 10000, // 총 10,000개 요청 (-n 10000)
    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const res = http.get(`${BASE_URL}/api/meow/boast-cat/recent`);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}