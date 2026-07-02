import http from "k6/http";
import { sleep, check } from "k6";

/**
 * 자랑글 상세조회 Cache Stampede 비교 — v2 (분산 락)
 *
 * 동일 postId에 트래픽 집중 → 캐시 MISS 시 첫 스레드만 DB 조회, 나머지 대기
 * 관측 포인트:
 *   - 서버 로그: "[v2 락 획득-DB 조회]" 1회 + "[v2 락 대기]" N회
 *   - v1 대비 DB 쿼리 수 급감, p95 레이턴시 안정화
 *
 * 실행 전: Redis 캐시 클리어
 *   redis-cli del "post:boast:detail::1"
 *
 * 실행: k6 run k6/boast-detail-stampede-v2-test.js
 */

export const options = {
    stages: [
        { duration: "1m", target: 50  },
        { duration: "1m", target: 100 },
        { duration: "1m", target: 200 },
        { duration: "1m", target: 300 },
        { duration: "1m", target: 500 },
        { duration: "30s", target: 0  },
    ],
};

const BASE_URL = "http://localhost:8080";
const POST_ID = 1;

export default function () {
    // v2: 분산 락 (MISS 시 첫 스레드만 DB 조회)
    const res = http.get(`${BASE_URL}/api/meow/boast-cat/v2/${POST_ID}`);
    check(res, { "200 OK": (r) => r.status === 200 });
    sleep(1);
}
