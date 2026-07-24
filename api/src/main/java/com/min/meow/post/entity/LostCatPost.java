package com.min.meow.post.entity;

import com.min.meow.common.BasePost;
import com.min.meow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Check;
// 위치 기반 조회용 JTS Point 타입 (Hibernate Spatial과 함께 동작)
import org.locationtech.jts.geom.Point;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lost_cat_post", indexes = {
    // 목록 조회: ORDER BY created_at DESC + 페이징
    @Index(name = "idx_lost_post_created_at", columnList = "created_at DESC"),
    // 마이페이지: WHERE user_id = ? ORDER BY created_at DESC
    @Index(name = "idx_lost_post_user_created_at", columnList = "user_id, created_at DESC")
})
@Check(constraints = "view >= 0 AND comment_count >= 0 AND (cat_age IS NULL OR (cat_age >= 0 AND cat_age <= 30)) " +
        "AND (cat_weight IS NULL OR (cat_weight >= 0 AND cat_weight <= 30)) AND (reward IS NULL OR reward >= 0)")
@SuperBuilder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
public class LostCatPost extends BasePost {

    // 목록 썸네일 URL (첫 번째 이미지, 없으면 null) — JOIN 없이 SELECT 가능
    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 20)
    private String catName;

    @Column(length = 30)
    private String catType;

    @Column(length = 20)
    private String catColor;

    private Integer catAge;
    private Integer catWeight;
    
    @Column(length = 10)
    private String catGender;
    private LocalDate lostDate;

    @Column(nullable = false, length = 100)
    private String lostLocation;

    private Double latitude;
    private Double longitude;

    // SPATIAL INDEX용 POINT 컬럼 (SRID 4326 = WGS 84)
    // bbox 방식(latitude/longitude)과 성능 비교용으로 함께 유지
    @Column(columnDefinition = "POINT SRID 4326")
    private Point location;

    private Integer reward;
    private boolean isCompleted;

    /**
     * 이미지 URL 목록 (@ElementCollection)
     * @BatchSize(100) 적용 이유:
     * - @ElementCollection은 1:N 관계와 동일하게 동작
     * - Fetch Join 시 카테시안 곱 발생 (게시글 데이터 중복)
     * - 목록 조회 시 N+1 문제 방지 (IN절로 배치 처리)
     * - 상세 조회 시에도 별도 쿼리 1개로 처리
     */
    @BatchSize(size = 100)
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    // 비정규화 필드: 조회 성능 최적화를 위해 카운트를 직접 저장
    @Builder.Default
    @ColumnDefault("0")
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

    // 엔티티 업데이트 메서드
    public void updatePost(String title, String contents, String catName, String catType,
                          String catColor, Integer catAge, Integer catWeight,
                          String catGender, LocalDate lostDate,
                          String lostLocation, Double latitude, Double longitude,
                          Point location, Integer reward, List<String> newImageUrls) {
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
        if (catGender != null) {
            this.catGender = catGender;
        }
        if (lostDate != null) {
            this.lostDate = lostDate;
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
        if (location != null) {
            this.location = location;
        }
        if (reward != null) {
            this.reward = reward;
        }
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
        }
    }

    // 작성자 확인 (ID 기반 비교로 영속성 컨텍스트에 의존하지 않음)
    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }

    // 완료 상태 변경
    public void markAsCompleted() {
        this.isCompleted = true;
    }

    // 완료 상태 토글 (찾는 중 ↔ 귀가 완료)
    public void setCompletedStatus(boolean completed) {
        this.isCompleted = completed;
    }
}