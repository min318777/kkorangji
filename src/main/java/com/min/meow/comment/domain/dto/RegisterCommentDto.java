package com.min.meow.comment.domain.dto;


import com.min.meow.comment.entity.LostCatPostComment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterCommentDto {

    private String content;

    public static RegisterCommentDto convertToDto(LostCatPostComment lostCatPostComment){
        return RegisterCommentDto.builder()
                .content(lostCatPostComment.getContent())
                .build();

    }
}
