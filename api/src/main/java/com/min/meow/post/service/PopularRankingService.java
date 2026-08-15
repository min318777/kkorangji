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

import java.util.List;
import java.util.Map;
import java.util.Set;

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
 *
 * 랭킹 키 전략: 현재는 트래픽이 적어 "전체 누적 순위"(키 하나, 만료 없음) 방식.
 * 트래픽이 늘어 "최근 인기글"만 보여주고 싶어지면 날짜별 키 로테이션으로 전환:
 *   1. getRankingKey()가 RANKING_KEY_PREFIX + LocalDate.now(ZoneId.of("Asia/Seoul"))를 반환하도록 변경
 *   2. 날짜가 바뀌면 새 키가 빈 Sorted Set으로 시작하므로, 자정마다 initRanking()과 같은 로직을
 *      실행하는 스케줄러(@Scheduled(cron = "0 0 0 * * *"))를 추가해 DB 기준으로 새 키를 재초기화해야 함
 *      (안 하면 어제까지의 인기글이 새 날짜 랭킹에서 사라지는 버그 재발 — 과거 실제로 겪음)
 *   3. 키 만료가 필요하면 예전처럼 setTtlIfAbsent()로 TTL을 다시 부여
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularRankingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PopularPostRepository popularPostRepository;

    // 누적 순위 방식: 날짜 접미사 없이 키 하나로 계속 쌓음 (만료 없음)
    private static final String RANKING_KEY = "post:boast:popular:ranking";

    private String getRankingKey() {
        return RANKING_KEY;
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
                return;
            }
            List<Tuple> posts = popularPostRepository.findTopForRankingInit(1000);
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
            log.info("[PopularRanking] 초기화 완료 - key={}, 상위 1000개 중 {}개 게시글", key, posts.size());
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
    }
}
