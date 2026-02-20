package com.min.meow.global;

import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class BasePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    // DB 레벨 제약 (검증은 Request DTO에서 처리)
    @Column(nullable = false, length = 100)
    protected String title;

    @Column(length = 2000)
    protected String contents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    protected User user;

    protected int view;

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 조회수 증가 (더티 체킹 방식)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * 이 방식은 read-modify-write 패턴으로 동시성 이슈가 발생합니다.
     *
     * 문제 시나리오 (현재 view = 100):
     * 1. Thread A: view 읽기 (100)
     * 2. Thread B: view 읽기 (100)
     * 3. Thread A: view++ → 101로 UPDATE
     * 4. Thread B: view++ → 101로 UPDATE (Thread A의 업데이트 덮어씀!)
     * 5. 결과: 2번 증가했지만 실제로는 1만 증가 (Lost Update)
     *
     * K6 동시성 테스트로 이 문제를 발견하여
     * 원자적 쿼리 방식(incrementViewCount)으로 개선하였습니다.
     *
     * @see com.min.meow.post.repository.LostCatRepository#incrementViewCount
     * @see com.min.meow.post.repository.BoastCatPostRepository#incrementViewCount
     */
    public void incrementView() {
        this.view++;
    }
}
