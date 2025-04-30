package com.min.meow.lostcatpost.domain.entity;


import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
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

    public void update(UpdateLostCatPostRequest request) {

        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }
        if (request.getContent() != null) {
            this.content = request.getContent();
        }
        if (request.getCatName() != null) {
            this.catName = request.getCatName();
        }
        if (request.getCatType() != null) {
            this.catType = request.getCatType();
        }
        if (request.getCatColor() != null) {
            this.catColor = request.getCatColor();
        }
        if (request.getCatAge() != null) {
            this.catAge = request.getCatAge();
        }
        if (request.getCatWeight() != null) {
            this.catWeight = request.getCatWeight();
        }
        if (request.getCatImageUrl() != null) {
            this.catImageUrl = request.getCatImageUrl();
        }
        if (request.getLostLocation() != null) {
            this.lostLocation = request.getLostLocation();
        }
        if (request.getLatitude() != null) {
            this.latitude = request.getLatitude();
        }
        if (request.getLongitude() != null) {
            this.longitude = request.getLongitude();
        }
        if (request.getReward() != null) {
            this.reward = request.getReward();
        }
    }
}
