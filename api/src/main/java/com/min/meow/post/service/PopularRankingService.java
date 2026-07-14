package com.min.meow.post.service;

import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.post.repository.PopularPostRepository;
import com.querydsl.core.Tuple;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 인기글 Sorted Set 관리 서비스
 * 키: post:boast:popular:ranking
 * MEMBER: postId (String)
 * SCORE:  likeCount×3 + commentCount×2 + view×1 (누적)
 *
 * 점수 갱신 시점:
 *  - 좋아요 추가/취소 → PopularScoreEvent(±3)
 *  - 댓글 추가/삭제  → PopularScoreEvent(±2)
 *  - 조회수 동기화   → ViewCountSyncScheduler → updateViewScores (30초 배치)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularRankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PopularPostRepository popularPostRepository;

    private static final String RANKING_KEY_PREFIX = "post:boast:popular:ranking:";
    // 오늘 키는 2일 후 자동 만료 (어제 데이터 fallback 용도)
    private static final long KEY_TTL_DAYS = 2;

    /** 오늘 날짜 기반 키 반환 (Asia/Seoul 기준) */
    private String getRankingKey() {
        return RANKING_KEY_PREFIX + LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    /** TTL이 설정되지 않은 키에만 만료 시간 부여 */
    private void setTtlIfAbsent(String key) {
        Long ttl = redisTemplate.getExpire(key);
        if (ttl != null && ttl == -1) {
            redisTemplate.expire(key, KEY_TTL_DAYS, TimeUnit.DAYS);
        }
    }

    /**
     * 서버 시작 시 Sorted Set 초기화
     * 이미 키가 존재하면 생략 (재시작 시 기존 데이터 유지)
     */
    @PostConstruct
    public void initRanking() {
        try {
            String key = getRankingKey();
            Long size = redisTemplate.opsForZSet().zCard(key);
            if (size != null && size > 0) {
                log.info("[PopularRanking] Sorted Set 이미 존재 - 초기화 생략 (key={}, size={})", key, size);
                setTtlIfAbsent(key);
                return;
            }
            List<Tuple> posts = popularPostRepository.findAllForRankingInit();
            for (Tuple post : posts) {
                Long id = post.get(0, Long.class);
                Integer likeCount = post.get(1, Integer.class);
                Integer commentCount = post.get(2, Integer.class);
                Integer view = post.get(3, Integer.class);
                if (id == null) continue;
                double score = (likeCount != null ? likeCount : 0) * 3.0
                        + (commentCount != null ? commentCount : 0) * 2.0
                        + (view != null ? view : 0);
                redisTemplate.opsForZSet().add(key, String.valueOf(id), score);
            }
            setTtlIfAbsent(key);
            log.info("[PopularRanking] 초기화 완료 - key={}, {}개 게시글", key, posts.size());
        } catch (Exception e) {
            log.error("[PopularRanking] 초기화 실패 - DB fallback으로 동작", e);
        }
    }

    /**
     * 좋아요/댓글 이벤트 수신 → ZINCRBY
     * 트랜잭션 커밋 후 비동기 처리 (Redis는 2PC 미지원)
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScoreEvent(PopularScoreEvent event) {
        try {
            String key = getRankingKey();
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(event.postId()), event.scoreDelta());
            setTtlIfAbsent(key);
            log.debug("[PopularRanking] ZINCRBY - postId: {}, delta: {}", event.postId(), event.scoreDelta());
        } catch (Exception e) {
            log.warn("[PopularRanking] ZINCRBY 실패 - postId: {}, delta: {}", event.postId(), event.scoreDelta(), e);
        }
    }

    /**
     * 상위 24개 게시글 ID 반환 (score 내림차순)
     */
    public List<Long> getTop24PostIds() {
        try {
            Set<String> members = redisTemplate.opsForZSet().reverseRange(getRankingKey(), 0, 23);
            if (members == null || members.isEmpty()) return List.of();
            return members.stream().map(Long::parseLong).toList();
        } catch (Exception e) {
            log.warn("[PopularRanking] getTop24PostIds 실패", e);
            return List.of();
        }
    }

    /**
     * 조회수 배치 동기화 시 Sorted Set 점수 갱신
     * ViewCountSyncScheduler가 DB 반영 후 호출
     * key 형식: "view:count:boast:{postId}"
     */
    public void updateViewScores(Map<String, Long> deltas) {
        if (deltas == null || deltas.isEmpty()) return;
        String rankingKey = getRankingKey();
        String prefix = "view:count:";
        for (Map.Entry<String, Long> entry : deltas.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.substring(prefix.length()).split(":");
            if (parts.length != 2 || !"boast".equalsIgnoreCase(parts[0])) continue;
            try {
                redisTemplate.opsForZSet().incrementScore(rankingKey, parts[1], entry.getValue());
            } catch (Exception e) {
                log.warn("[PopularRanking] 조회수 점수 갱신 실패 - key: {}", key, e);
            }
        }
        setTtlIfAbsent(rankingKey);
    }
}
