package com.min.meow.post.entity;


import com.min.meow.post.comment.entity.Comment;
import com.min.meow.global.BasePost;
import com.min.meow.post.domain.request.CreateLostCatPostRequest;
import com.min.meow.post.domain.request.UpdateLostCatPostRequest;
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

    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    private String lostLocation;

    private Double latitude;

    private Double longitude;

    private Integer reward;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "lostCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public static LostCatPost toEntity(CreateLostCatPostRequest createLostCatPostRequest, List<String> imageUrls, User user) {
        return LostCatPost.builder()
                .title(createLostCatPostRequest.getTitle())
                .contents(createLostCatPostRequest.getContent())
                .user(user)
                .lostLocation(createLostCatPostRequest.getLostLocation())
                .latitude(createLostCatPostRequest.getLatitude())
                .longitude(createLostCatPostRequest.getLongitude())
                .catName(createLostCatPostRequest.getCatName())
                .catAge(createLostCatPostRequest.getCatAge())
                .catType(createLostCatPostRequest.getCatType())
                .catWeight(createLostCatPostRequest.getCatWeight())
                .catColor(createLostCatPostRequest.getCatColor())
                .imageUrls(imageUrls)
                .reward(createLostCatPostRequest.getReward())
                .build();
    }

    public void update(UpdateLostCatPostRequest updateLostCatPostRequest) {

        if (updateLostCatPostRequest.getTitle() != null) {
            this.title = updateLostCatPostRequest.getTitle();
        }
        if (updateLostCatPostRequest.getContent() != null) {
            this.contents = updateLostCatPostRequest.getContent();
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
        if (updateLostCatPostRequest.getImageUrls() != null) {
            this.imageUrls = updateLostCatPostRequest.getImageUrls();
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

    public void plusView(){this.view += 1;}

}
