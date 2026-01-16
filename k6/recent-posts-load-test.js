/**
 * k6 성능 테스트 스크립트 - 메인페이지 최근 게시물 API
 *
 * 이 스크립트는 캐싱 적용/미적용 상태에서의 API 성능을 비교하기 위한 부하 테스트입니다.
 *
 * 테스트 방법:
 * 1. k6 설치 (https://k6.io/docs/getting-started/installation/)
 * 2. 테스트 실행: k6 run k6/recent-posts-load-test.js
 *
 * 캐싱 비교 테스트 방법:
 * 1. 캐싱 적용 테스트:
 *    - BoastCatPostServiceImpl.getRecentBoastCatPosts()의 @Cacheable 유지
 *    - LostCatPostServiceImpl.getRecentLostCatPosts()의 @Cacheable 유지
 *    - 테스트 실행 후 결과 기록
 *
 * 2. 캐싱 미적용 테스트:
 *    - 위 메서드들의 @Cacheable 어노테이션을 주석 처리
 *    - 서버 재시작
 *    - 테스트 실행 후 결과 비교
 *
 * 주요 측정 지표:
 * - http_req_duration: 요청 응답 시간 (p95, p99 중요)
 * - http_reqs: 초당 처리 요청 수 (throughput)
 * - http_req_failed: 실패율
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
const boastPostsLatency = new Trend('boast_posts_latency');  // 자랑글 API 응답 시간
const lostPostsLatency = new Trend('lost_posts_latency');    // 실종글 API 응답 시간
const errorRate = new Rate('errors');                         // 에러 비율
const successfulRequests = new Counter('successful_requests'); // 성공한 요청 수

// 테스트 설정
export const options = {
    // 부하 테스트 시나리오
    scenarios: {
        // 시나리오 1: 점진적 부하 증가 (Ramp-up)
        ramp_up_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },   // 30초 동안 0 -> 50 VU로 증가
                { duration: '1m', target: 50 },    // 1분간 50 VU 유지
                { duration: '30s', target: 100 },  // 30초 동안 50 -> 100 VU로 증가
                { duration: '1m', target: 100 },   // 1분간 100 VU 유지
                { duration: '30s', target: 0 },    // 30초 동안 0으로 감소
            ],
            gracefulRampDown: '10s',
        },
    },

    // 성능 기준 (Thresholds)
    thresholds: {
        // 전체 요청의 95%가 500ms 이내에 응답해야 함
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        // 자랑글 API 응답 시간
        boast_posts_latency: ['p(95)<300', 'p(99)<500'],
        // 실종글 API 응답 시간
        lost_posts_latency: ['p(95)<300', 'p(99)<500'],
        // 에러율 1% 미만
        errors: ['rate<0.01'],
        // 요청 실패율 1% 미만
        http_req_failed: ['rate<0.01'],
    },
};

// 기본 URL 설정 (환경에 맞게 수정)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// API 엔드포인트
const ENDPOINTS = {
    recentBoastPosts: `${BASE_URL}/api/meow/boast-cat/recent`,
    recentLostPosts: `${BASE_URL}/api/meow/lost-cat/recent`,
};

// 테스트 시작 전 준비 (선택사항)
export function setup() {
    console.log('='.repeat(60));
    console.log('메인페이지 최근 게시물 API 부하 테스트 시작');
    console.log(`대상 서버: ${BASE_URL}`);
    console.log('='.repeat(60));

    // 서버 상태 확인
    const healthCheck = http.get(`${ENDPOINTS.recentBoastPosts}`);
    if (healthCheck.status !== 200) {
        console.warn(`경고: 서버 응답 상태 ${healthCheck.status}`);
    }

    return { startTime: new Date().toISOString() };
}

// 메인 테스트 함수 (각 VU가 반복 실행)
export default function () {
    // 1. 자랑글 최근 20개 조회
    const boastResponse = http.get(ENDPOINTS.recentBoastPosts, {
        tags: { name: 'RecentBoastPosts' },
    });

    // 응답 검증
    const boastCheck = check(boastResponse, {
        '자랑글 API 상태코드 200': (r) => r.status === 200,
        '자랑글 API 응답 성공': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'OK';
            } catch (e) {
                return false;
            }
        },
        '자랑글 데이터 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && Array.isArray(body.data);
            } catch (e) {
                return false;
            }
        },
    });

    // 메트릭 기록
    boastPostsLatency.add(boastResponse.timings.duration);
    if (boastCheck) {
        successfulRequests.add(1);
    } else {
        errorRate.add(1);
    }

    // 짧은 대기 (실제 사용자 시뮬레이션)
    sleep(0.1);

    // 2. 실종글 최근 20개 조회
    const lostResponse = http.get(ENDPOINTS.recentLostPosts, {
        tags: { name: 'RecentLostPosts' },
    });

    // 응답 검증
    const lostCheck = check(lostResponse, {
        '실종글 API 상태코드 200': (r) => r.status === 200,
        '실종글 API 응답 성공': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'OK';
            } catch (e) {
                return false;
            }
        },
        '실종글 데이터 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && Array.isArray(body.data);
            } catch (e) {
                return false;
            }
        },
    });

    // 메트릭 기록
    lostPostsLatency.add(lostResponse.timings.duration);
    if (lostCheck) {
        successfulRequests.add(1);
    } else {
        errorRate.add(1);
    }

    // 다음 반복까지 대기 (실제 사용자처럼 행동)
    sleep(Math.random() * 0.5 + 0.5);  // 0.5 ~ 1초 랜덤 대기
}

// 테스트 종료 후 정리
export function teardown(data) {
    console.log('='.repeat(60));
    console.log('테스트 완료');
    console.log(`시작 시간: ${data.startTime}`);
    console.log(`종료 시간: ${new Date().toISOString()}`);
    console.log('='.repeat(60));
}

/**
 * 테스트 결과 분석 가이드
 *
 * 1. 캐싱 적용 시 기대 결과:
 *    - http_req_duration p95 < 50ms (캐시 히트 시)
 *    - 높은 throughput (초당 수천 요청 처리 가능)
 *    - 일정한 응답 시간 (분산이 낮음)
 *
 * 2. 캐싱 미적용 시 예상 결과:
 *    - http_req_duration p95 > 100ms (DB 조회마다)
 *    - 낮은 throughput (DB 연결 제한)
 *    - DB 부하에 따른 응답 시간 변동
 *
 * 3. 비교 포인트:
 *    - p95 응답 시간 차이
 *    - 최대 처리 가능 요청 수
 *    - 에러율 차이
 *
 * 결과 저장:
 * k6 run --out json=results.json k6/recent-posts-load-test.js
 */
