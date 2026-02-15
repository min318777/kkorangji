package com.min.meow.post.postlike.service;

import com.min.meow.global.PostType;

/**
 * Redis 기반 좋아요 관리 서비스 인터페이스
 *
 * 좋아요 처리 방식 비교:
 * ┌─────────────────┬───────────────────────────────────────────────────────────────┐
 * │ 방식            │ 특징                                                          │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ v1 더티체킹     │ Entity 조회 → Java에서 증감 → JPA flush                       │
 * │ (현재 방식)     │ ❌ 동시성 이슈 (Lost Update)                                  │
 * │                 │ ❌ 매 요청마다 SELECT + UPDATE 쿼리                          │
 * ├─────────────────┼───────────────────────────────────────────────────────────────┤
 * │ v2 Redis+SET    │ Redis SET으로 좋아요 사용자 관리 + INCR로 카운트             │
 * │ (개선 방식)     │ ✅ 동시성 안전 (Redis Single-threaded)                       │
 * │                 │ ✅ DB 부하 대폭 감소 (배치 동기화)                           │
 * │                 │ ✅ 초고속 응답 (메모리 연산)                                  │
 * │                 │ ✅ 중복 좋아요 방지 (SET 자료구조)                           │
 * └─────────────────┴───────────────────────────────────────────────────────────────┘
 *
 * Redis 자료구조:
 * - 좋아요 사용자 SET: like:users:{postType}:{postId} → {userId1, userId2, ...}
 * - 좋아요 수 변경분: like:delta:{postType}:{postId} → 증가/감소 누적값
 *
 * 동기화 전략:
 * - 스케줄러가 주기적으로 (예: 1분) Redis의 delta를 DB에 반영
 * - PostLike 테이블은 DB 동기화 시 일괄 처리
 */
public interface LikeCountService {

    /**
     * 좋아요 토글 - Redis 방식 (v2 - 개선 버전)
     *
     * Redis SET을 사용하여 좋아요 상태를 관리합니다.
     * - SISMEMBER로 기존 좋아요 여부 확인 (O(1))
     * - 있으면: SREM으로 제거 + delta 감소
     * - 없으면: SADD로 추가 + delta 증가
     *
     * @param postType 게시글 타입 (BOAST, LOST)
     * @param postId 게시글 ID
     * @param userId 사용자 ID (PK)
     * @return true: 좋아요 등록됨, false: 좋아요 취소됨
     */
    boolean toggleLike(PostType postType, Long postId, Long userId);

    /**
     * 좋아요 여부 확인
     *
     * Redis SET에서 해당 사용자의 좋아요 여부를 확인합니다.
     * 시간복잡도: O(1)
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태
     */
    boolean isLiked(PostType postType, Long postId, Long userId);

    /**
     * 현재 좋아요 수 조회 (Redis 기준)
     *
     * Redis에서 현재 좋아요 사용자 수를 조회합니다.
     * SET의 SCARD 명령어 사용 (O(1))
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @return 좋아요 수
     */
    Long getLikeCount(PostType postType, Long postId);

    /**
     * Redis 좋아요 데이터를 DB에 동기화 (스케줄러에서 호출)
     *
     * 동기화 내용:
     * 1. likeCount delta값을 DB에 반영 (원자적 UPDATE)
     * 2. PostLike 테이블에 새로운 좋아요 INSERT / 취소된 좋아요 DELETE
     * 3. 동기화 완료 후 delta 키 삭제
     */
    void syncLikesToDatabase();

    /**
     * 특정 게시글의 Redis 좋아요 데이터를 DB에 동기화
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     */
    void syncLikeToDatabase(PostType postType, Long postId);

    /**
     * Redis에 게시글의 기존 좋아요 데이터 로드 (캐시 워밍)
     *
     * DB의 PostLike 데이터를 Redis SET에 로드합니다.
     * 서버 시작 시 또는 캐시 미스 시 호출됩니다.
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     */
    void warmUpCache(PostType postType, Long postId);
}
