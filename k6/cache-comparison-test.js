/**
 * 캐싱 성능 비교 테스트 (간단 버전)
 *
 * 실행:
 *   캐싱 O: k6 run --env TEST_NAME=WITH_CACHE k6/cache-comparison-test.js
 *   캐싱 X: k6 run --env TEST_NAME=NO_CACHE k6/cache-comparison-test.js
 */

import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        load_test: {
            executor: 'constant-arrival-rate',
            rate: 100,                // 초당 100 요청 (병목 없이 정상 비교)
            timeUnit: '1s',
            duration: '1m',           // 1분
            preAllocatedVUs: 50,
            maxVUs: 100,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_NAME = __ENV.TEST_NAME || 'UNKNOWN';

export function setup() {
    console.log(`\n🚀 테스트 시작: ${TEST_NAME}`);
    // 캐시 워밍
    for (let i = 0; i < 3; i++) {
        http.get(`${BASE_URL}/api/meow/boast-cat/recent`);
    }
}

export default function () {
    const res = http.get(`${BASE_URL}/api/meow/boast-cat/recent`);
    check(res, { 'status 200': (r) => r.status === 200 });
}

// 결과 요약 출력
export function handleSummary(data) {
    const rps = Math.round(data.metrics.http_reqs.values.rate * 100) / 100;
    const p95 = Math.round(data.metrics.http_req_duration.values['p(95)']);
    const p99 = Math.round(data.metrics.http_req_duration.values['p(99)']);
    const avg = Math.round(data.metrics.http_req_duration.values.avg);
    const errorRate = Math.round(data.metrics.http_req_failed.values.rate * 10000) / 100;
    const total = data.metrics.http_reqs.values.count;

    const report = `
╔════════════════════════════════════════════════════════════╗
║              📊 ${TEST_NAME} 테스트 결과
╠════════════════════════════════════════════════════════════╣
║  [Throughput]  RPS: ${rps} req/sec (총 ${total}건)
║  [Latency]     평균: ${avg}ms | p95: ${p95}ms | p99: ${p99}ms
║  [Error Rate]  ${errorRate}%
╠════════════════════════════════════════════════════════════╣
║  📝 포트폴리오 기재용:
║  "p95 응답시간 ${p95}ms, 처리량 ${rps} RPS, 에러율 ${errorRate}%"
╚════════════════════════════════════════════════════════════╝
`;

    return { 'stdout': report };
}
