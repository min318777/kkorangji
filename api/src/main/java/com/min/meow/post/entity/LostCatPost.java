package com.min.meow.post.entity;

import com.min.meow.comment.entity.Comment;
import com.min.meow.global.BasePost;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LostCatPost extends BasePost {

    // DB 레벨 제약 (검증은 Request DTO에서 처리)
    @Column(length = 20)
    private String catName;

    @Column(length = 30)
    private String catType;

    @Column(length = 20)
    private String catColor;

    private Integer catAge;
    private Integer catWeight;

    @Column(length = 100)
    private String lostLocation;

    private Double latitude;
    private Double longitude;
    private Integer reward;
    private boolean isCompleted;

    /**
     * 이미지 URL 목록 (@ElementCollection)
     *
     * @BatchSize(100) 적용 이유:
     * - @ElementCollection은 1:N 관계와 동일하게 동작
     * - Fetch Join 시 카테시안 곱 발생 (게시글 데이터 중복)
     * - 목록 조회 시 N+1 문제 방지 (IN절로 배치 처리)
     * - 상세 조회 시에도 별도 쿼리 1개로 처리
     */
    @BatchSize(size = 100)
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "lostCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 비정규화 필드: 조회 성능 최적화를 위해 카운트를 직접 저장
    @Builder.Default
    private int commentCount = 0;  // 댓글 수

    // 댓글 수 증가
    public void incrementCommentCount() {
        this.commentCount++;
    }

    // 댓글 수 감소
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // 엔티티 업데이트 메서드 (순수 데이터 모델 유지)
    public void updatePost(String title, String contents, String catName, String catType,
                          String catColor, Integer catAge, Integer catWeight,
                          String lostLocation, Double latitude, Double longitude,
                          Integer reward, List<String> newImageUrls) {
        if (title != null) {
            this.title = title;
        }
        if (contents != null) {
            this.contents = contents;
        }
        if (catName != null) {
            this.catName = catName;
        }
        if (catType != null) {
            this.catType = catType;
        }
        if (catColor != null) {
            this.catColor = catColor;
        }
        if (catAge != null) {
            this.catAge = catAge;
        }
        if (catWeight != null) {
            this.catWeight = catWeight;
        }
        if (lostLocation != null) {
            this.lostLocation = lostLocation;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
        if (reward != null) {
            this.reward = reward;
        }
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
        }
    }

    // 작성자 확인
    public boolean isAuthor(User user) {
        return this.user.equals(user);
    }

    // 완료 상태 변경
    public void markAsCompleted() {
        this.isCompleted = true;
    }
}