package com.min.meow.post.service;

import com.min.meow.global.PostType;

/**
 * Redis 기반 조회수 관리 서비스 인터페이스
 *
 * 조회수 처리 방식 비교:
 * ┌─────────────────┬───────────────────────────────────────────────────────────────┐
 * │ 방식            │ 특징                                                          │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ v1 더티체킹     │ Entity 조회 → Java에서 증가 → JPA flush                       │
 * │                 │ ❌ 동시성 이슈 (Lost Update)                                  │
 * │                 │ ❌ 매 요청마다 SELECT + UPDATE 쿼리                          │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ v2 원자적쿼리   │ UPDATE view = view + 1 (DB 레벨 원자적)                      │
 * │                 │ ✅ 동시성 안전                                                │
 * │                 │ ⚠️ 매 요청마다 DB UPDATE 쿼리                               │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ v3 Redis+INCR   │ Redis INCR (메모리 기반 원자적 증가)                         │
 * │                 │ ✅ 동시성 안전                                                │
 * │                 │ ✅ DB 부하 대폭 감소 (배치 동기화)                           │
 * │                 │ ✅ 초고속 응답 (메모리 연산)                                  │
 * └─────────────────┴───────────────────────────────────────────────────────────────┘
 *
 * Redis 조회수 동기화 전략:
 * - 스케줄러가 주기적으로 (예: 1분) Redis의 조회수를 DB에 반영
 * - Redis 장애 시 CacheErrorHandler로 DB 직접 업데이트 fallback
 * - 서버 재시작 시에도 마지막 동기화 이후 데이터만 손실 (허용 범위)
 */
public interface ViewCountService {

    /**
     * 조회수 증가 - Redis INCR 방식 (v3 - 최적화 버전)
     *
     * Redis의 INCR 명령어를 사용하여 조회수를 원자적으로 증가시킵니다.
     * INCR는 single-threaded인 Redis에서 원자적으로 실행되므로
     * 동시성 문제가 발생하지 않습니다.
     *
     * Redis 키 형식: view:count:{postType}:{postId}
     * 예: view:count:BOAST:123
     *
     * @param postType 게시글 타입 (BOAST, LOST)
     * @param postId 게시글 ID
     * @return 증가 후 조회수
     */
    Long incrementViewCount(PostType postType, Long postId);

    /**
     * 현재 조회수 조회
     *
     * Redis에서 현재 조회수를 조회합니다.
     * Redis에 데이터가 없으면 DB에서 조회하여 반환합니다.
     *
     * @param postType 게시글 타입 (BOAST, LOST)
     * @param postId 게시글 ID
     * @return 현재 조회수
     */
    Long getViewCount(PostType postType, Long postId);

    /**
     * Redis 조회수를 DB에 동기화 (스케줄러에서 호출)
     *
     * Redis에 저장된 모든 조회수 증가분을 DB에 반영합니다.
     * 배치 처리로 DB 부하를 최소화합니다.
     *
     * 동기화 후 해당 Redis 키는 삭제됩니다.
     */
    void syncViewCountsToDatabase();

    /**
     * 특정 게시글의 Redis 조회수를 DB에 동기화
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     */
    void syncViewCountToDatabase(PostType postType, Long postId);
}
