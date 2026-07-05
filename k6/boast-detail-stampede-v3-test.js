import http from "k6/http";
import { sleep, check } from "k6";

/**
 * 인기글 상세조회 Cache Stampede 비교 — v3 (Cache Warming)
 *
 * DetailCacheWarmingScheduler가 25초마다 TOP 24 상세 캐시를 선제 갱신
 * → TTL 만료 전 항상 캐시가 채워져 있음 → MISS 자체 발생하지 않음
 *
 * 관측 포인트:
 *   - 서버 로그: "[v3 Cache MISS]" 거의 출력되지 않음 (서버 시작 직후 제외)
 *   - v1/v2 대비 p95 레이턴시 최저, DB 쿼리 수 최소
 *
 * 실행 전: 서버 시작 후 25초 대기 (워밍 완료 후 테스트)
 *
 * 실행: k6 run k6/boast-detail-stampede-v3-test.js
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
    // v3: 인기글 상세 Cache Warming (스케줄러가 선제 갱신)
    const res = http.get(`${BASE_URL}/api/meow/boast-cat/popular/detail/v3/${POST_ID}`);
    check(res, { "200 OK": (r) => r.status === 200 });
    sleep(1);
}
