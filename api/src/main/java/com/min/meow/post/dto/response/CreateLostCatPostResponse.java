package com.min.meow.post.dto.response;

import com.min.meow.post.entity.LostCatPost;
import com.min.meow.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private String catName;
    private String writer;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private List<String> imageUrls;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CreateLostCatPostResponse toResponse(LostCatPost lostCatPost, User user){

        return CreateLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContents())
                .catName(lostCatPost.getCatName())
                .writer(user.getLoginId())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .imageUrls(lostCatPost.getImageUrls())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
