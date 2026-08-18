package com.min.meow.post.service;

import com.min.meow.post.repository.PopularPostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PopularRankingService 유닛 테스트")
class PopularRankingServiceTest {

    @InjectMocks
    private PopularRankingService popularRankingService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private PopularPostRepository popularPostRepository;

    @Nested
    @DisplayName("removeFromRanking() — 게시글 삭제 시 랭킹에서 제거")
    class RemoveFromRanking {

        @Test
        @DisplayName("성공: 지정한 postId를 Sorted Set에서 제거한다")
        void test_성공_랭킹에서_제거() {
            // given
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

            // when
            popularRankingService.removeFromRanking(42L);

            // then
            then(zSetOperations).should().remove("post:boast:popular:ranking", "42");
        }

        @Test
        @DisplayName("성공: Redis 장애로 예외가 발생해도 예외를 밖으로 던지지 않는다")
        void test_성공_Redis_장애시_예외_흡수() {
            // given
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(zSetOperations.remove("post:boast:popular:ranking", "42"))
                    .willThrow(new RuntimeException("Redis 연결 끊김"));

            // when & then — 예외 없이 정상 반환
            popularRankingService.removeFromRanking(42L);
        }
    }

    @Nested
    @DisplayName("updateViewScores() — 조회수 배치 반영 시 점수 갱신")
    class UpdateViewScores {

        @Test
        @DisplayName("성공: boast 접두사 키만 랭킹 점수에 반영한다")
        void test_성공_boast만_반영() {
            // given
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            Map<String, Long> deltas = new LinkedHashMap<>();
            deltas.put("view:count:boast:1", 5L);
            deltas.put("view:count:lost:2", 10L);

            // when
            popularRankingService.updateViewScores(deltas);

            // then
            then(zSetOperations).should().incrementScore("post:boast:popular:ranking", "1", 5L);
            then(zSetOperations).should(never()).incrementScore("post:boast:popular:ranking", "2", 10L);
        }

        @Test
        @DisplayName("성공: 빈 맵이면 아무 것도 하지 않는다")
        void test_성공_빈_맵_무시() {
            // when
            popularRankingService.updateViewScores(Map.of());

            // then
            then(redisTemplate).should(never()).opsForZSet();
        }

        @Test
        @DisplayName("성공: null이면 예외 없이 그냥 종료한다")
        void test_성공_null_무시() {
            // when & then
            popularRankingService.updateViewScores(null);
            then(redisTemplate).should(never()).opsForZSet();
        }
    }

    @Nested
    @DisplayName("getTop24PostIds() — 상위 24개 조회")
    class GetTop24PostIds {

        @Test
        @DisplayName("성공: Sorted Set의 멤버 문자열을 Long ID 목록으로 변환한다")
        void test_성공_ID_목록_변환() {
            // given
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            Set<String> members = new LinkedHashSet<>(List.of("3", "1", "2"));
            given(zSetOperations.reverseRange("post:boast:popular:ranking", 0, 23)).willReturn(members);

            // when
            List<Long> result = popularRankingService.getTop24PostIds();

            // then
            assertThat(result).containsExactly(3L, 1L, 2L);
        }

        @Test
        @DisplayName("성공: Sorted Set이 비어있으면 빈 목록을 반환한다")
        void test_성공_빈_목록() {
            // given
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(zSetOperations.reverseRange("post:boast:popular:ranking", 0, 23)).willReturn(Set.of());

            // when
            List<Long> result = popularRankingService.getTop24PostIds();

            // then
            assertThat(result).isEmpty();
        }
    }
}
