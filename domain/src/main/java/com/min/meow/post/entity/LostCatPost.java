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

    private String catName;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    private boolean isCompleted;

    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "lostCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

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

    // 조회수 증가
    public void increaseView() {
        this.view += 1;
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