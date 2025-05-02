package com.min.meow.lostcatpost.entity;


import com.min.meow.lostcatpostcomment.entity.LostCatPostComment;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LostCatPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lostCatPostId;

    @Column(nullable = false)
    private String title;

    private String content;

    private String catName;

    private String catType;

    private String catColor;

    private Integer catAge;

    private Integer catWeight;

    private String catImageUrl;

    private String lostLocation;

    private Double latitude;

    private Double longitude;

    private Integer reward;

    @OneToMany(mappedBy = "lostCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LostCatPostComment> lostCatPostComments = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static LostCatPost convertToEntity(CreateLostCatPostRequest createLostCatPostRequest) {
        return LostCatPost.builder()
                .title(createLostCatPostRequest.getTitle())
                .content(createLostCatPostRequest.getContent())
                .lostLocation(createLostCatPostRequest.getLostLocation())
                .latitude(createLostCatPostRequest.getLatitude())
                .longitude(createLostCatPostRequest.getLongitude())
                .catName(createLostCatPostRequest.getCatName())
                .catAge(createLostCatPostRequest.getCatAge())
                .catType(createLostCatPostRequest.getCatType())
                .catWeight(createLostCatPostRequest.getCatWeight())
                .catColor(createLostCatPostRequest.getCatColor())
                .catImageUrl(createLostCatPostRequest.getCatImageUrl())
                .reward(createLostCatPostRequest.getReward())
                .build();
    }

    public void update(UpdateLostCatPostRequest updateLostCatPostRequest) {

        if (updateLostCatPostRequest.getTitle() != null) {
            this.title = updateLostCatPostRequest.getTitle();
        }
        if (updateLostCatPostRequest.getContent() != null) {
            this.content = updateLostCatPostRequest.getContent();
        }
        if (updateLostCatPostRequest.getCatName() != null) {
            this.catName = updateLostCatPostRequest.getCatName();
        }
        if (updateLostCatPostRequest.getCatType() != null) {
            this.catType = updateLostCatPostRequest.getCatType();
        }
        if (updateLostCatPostRequest.getCatColor() != null) {
            this.catColor = updateLostCatPostRequest.getCatColor();
        }
        if (updateLostCatPostRequest.getCatAge() != null) {
            this.catAge = updateLostCatPostRequest.getCatAge();
        }
        if (updateLostCatPostRequest.getCatWeight() != null) {
            this.catWeight = updateLostCatPostRequest.getCatWeight();
        }
        if (updateLostCatPostRequest.getCatImageUrl() != null) {
            this.catImageUrl = updateLostCatPostRequest.getCatImageUrl();
        }
        if (updateLostCatPostRequest.getLostLocation() != null) {
            this.lostLocation = updateLostCatPostRequest.getLostLocation();
        }
        if (updateLostCatPostRequest.getLatitude() != null) {
            this.latitude = updateLostCatPostRequest.getLatitude();
        }
        if (updateLostCatPostRequest.getLongitude() != null) {
            this.longitude = updateLostCatPostRequest.getLongitude();
        }
        if (updateLostCatPostRequest.getReward() != null) {
            this.reward = updateLostCatPostRequest.getReward();
        }
    }
}
