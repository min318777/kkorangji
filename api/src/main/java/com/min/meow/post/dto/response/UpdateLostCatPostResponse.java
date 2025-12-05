package com.min.meow.post.dto.response;

import com.min.meow.comment.dto.CommentDto;
import com.min.meow.post.entity.LostCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private String catName;
    private int view;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private List<String> imageUrls;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    private List<CommentDto> commentDtoList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdateLostCatPostResponse toResponse(LostCatPost lostCatPost){

        return UpdateLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .view(lostCatPost.getView())
                .content(lostCatPost.getContents())
                .catName(lostCatPost.getCatName())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .imageUrls(lostCatPost.getImageUrls())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .commentDtoList(lostCatPost.getComments().stream().map(CommentDto::toDto).collect(Collectors.toList()))
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
