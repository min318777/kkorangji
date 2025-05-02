package com.min.meow.lostcatpost.domain.dto;

import com.min.meow.comment.domain.dto.LostCatPostCommentDto;
import com.min.meow.comment.entity.LostCatPostComment;
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
public class LostCatPostDto {

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

    private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static LostCatPostDto convertToDto(LostCatPost lostCatPost){

        return LostCatPostDto.builder()
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
                .lostCatPostCommentDtos(lostCatPost.getLostCatPostComments().stream().map(LostCatPostCommentDto::convertToDto).collect(Collectors.toList()))
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
