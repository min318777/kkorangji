import http from "k6/http";
import { sleep, check } from "k6";

/**
 * 인기글 상세조회 Cache Stampede 비교 — v1 (기본 @Cacheable)
 *
 * 동일 postId 1개에 트래픽 집중 → TTL 만료 시 동시 요청 → 여러 스레드 DB 조회
 * 관측 포인트:
 *   - 서버 로그: "[v1 Cache MISS]" 출력 횟수 = DB 접근 횟수
 *   - Grafana p95 레이턴시, DB slow log
 *
 * 실행 전: Redis 캐시 클리어
 *   redis-cli del "post:boast:detail::1"
 *
 * 실행: k6 run k6/boast-detail-stampede-v1-test.js
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
// 동일 게시글에 집중 (Stampede 재현)
const POST_ID = 1;

export default function () {
    // v1: 인기글 상세 기본 캐시 (Stampede 방지 없음)
    const res = http.get(`${BASE_URL}/api/meow/boast-cat/popular/detail/${POST_ID}`);
    check(res, { "200 OK": (r) => r.status === 200 });
    sleep(1);
}
