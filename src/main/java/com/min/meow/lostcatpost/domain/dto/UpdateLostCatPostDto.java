package com.min.meow.lostcatpost.domain.dto;

import com.min.meow.postcomment.domain.dto.PostCommentDto;
import com.min.meow.lostcatpost.entity.LostCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostCatPostDto {

    private Long lostCatPostId;

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

    private List<PostCommentDto> postCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UpdateLostCatPostDto convertToDto(LostCatPost lostCatPost){

        return UpdateLostCatPostDto.builder()
                .lostCatPostId(lostCatPost.getLostCatPostId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContent())
                .catName(lostCatPost.getCatName())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .catImageUrl(lostCatPost.getCatImageUrl())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .postCommentDtos(lostCatPost.getPostComments().stream().map(PostCommentDto::convertToDto).collect(Collectors.toList()))
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
