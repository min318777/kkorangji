package com.min.meow.post.service;

import com.min.meow.common.PostType;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViewCountService 유닛 테스트")
class ViewCountServiceTest {

    @InjectMocks
    private ViewCountService viewCountService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    @Nested
    @DisplayName("incrementViewCount() — 조회수 증가")
    class IncrementViewCount {

        @Test
        @DisplayName("성공: 존재하는 게시글이고 어뷰징 락도 통과하면 Redis 카운트를 증가시킨다")
        void test_성공_정상_증가() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);
            given(boastCatPostRepository.existsById(1L)).willReturn(true);
            given(valueOperations.increment("view:count:boast:1")).willReturn(5L);

            // when
            Long result = viewCountService.incrementViewCount(PostType.BOAST, 1L, "ip:127.0.0.1");

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("무시: 10분 내 같은 식별자로 재조회하면(어뷰징 락) 카운트 증가 없이 null을 반환한다")
        void test_무시_어뷰징_락() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(false);

            // when
            Long result = viewCountService.incrementViewCount(PostType.BOAST, 1L, "ip:127.0.0.1");

            // then
            assertThat(result).isNull();
            then(boastCatPostRepository).should(never()).existsById(any());
            then(valueOperations).should(never()).increment(anyString());
        }

        @Test
        @DisplayName("무시(회귀 방지): 존재하지 않는 게시글이면 카운트 키를 만들지 않고 null을 반환한다")
        void test_무시_존재하지_않는_게시글() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);
            given(boastCatPostRepository.existsById(9999L)).willReturn(false);

            // when
            Long result = viewCountService.incrementViewCount(PostType.BOAST, 9999L, "ip:127.0.0.1");

            // then
            assertThat(result).isNull();
            then(valueOperations).should(never()).increment(anyString());
        }

        @Test
        @DisplayName("실패: Redis 증가가 예외를 던지면 DB에 직접 +1 반영하는 fallback으로 전환한다")
        void test_성공_Redis_장애시_DB_fallback() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).willReturn(true);
            given(lostCatRepository.existsById(1L)).willReturn(true);
            given(valueOperations.increment("view:count:lost:1"))
                    .willThrow(new RuntimeException("Redis 연결 끊김"));

            // when
            Long result = viewCountService.incrementViewCount(PostType.LOST, 1L, "user:10");

            // then
            assertThat(result).isEqualTo(1L);
            then(lostCatRepository).should().incrementViewCount(1L);
        }
    }

    @Nested
    @DisplayName("getViewCount() — Redis 미반영 증가분 조회")
    class GetViewCount {

        @Test
        @DisplayName("성공: 저장된 값이 있으면 숫자로 변환하여 반환한다")
        void test_성공_값_있음() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("view:count:boast:1")).willReturn("7");

            // when
            Long result = viewCountService.getViewCount(PostType.BOAST, 1L);

            // then
            assertThat(result).isEqualTo(7L);
        }

        @Test
        @DisplayName("성공: 저장된 값이 없으면 0을 반환한다")
        void test_성공_값_없음() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("view:count:boast:1")).willReturn(null);

            // when
            Long result = viewCountService.getViewCount(PostType.BOAST, 1L);

            // then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("성공: Redis 조회 중 예외가 발생해도 0을 반환하며 예외를 전파하지 않는다")
        void test_성공_Redis_장애시_0_반환() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString())).willThrow(new RuntimeException("Redis 연결 끊김"));

            // when
            Long result = viewCountService.getViewCount(PostType.BOAST, 1L);

            // then
            assertThat(result).isZero();
        }
    }
}
