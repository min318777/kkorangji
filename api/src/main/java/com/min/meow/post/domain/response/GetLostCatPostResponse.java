package com.min.meow.post.domain.response;

import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.comment.domain.CommentDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetLostCatPostResponse {

    private Long id;

    private String title;

    private String content;

    private String writer;

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

    private List<CommentDto> commentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static GetLostCatPostResponse convertToResponse(LostCatPost lostCatPost){

        return GetLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContents())
                .catName(lostCatPost.getCatName())
                .writer(lostCatPost.getUser().getLoginId())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .catImageUrl(lostCatPost.getCatImageUrl())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .commentDtos(lostCatPost.getComments().stream().map(CommentDto::convertToDto).collect(Collectors.toList()))
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
