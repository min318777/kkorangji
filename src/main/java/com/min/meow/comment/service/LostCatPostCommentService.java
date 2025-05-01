package com.min.meow.comment.service;


import com.min.meow.comment.domain.dto.RegisterCommentDto;
import com.min.meow.comment.domain.request.RegisterCommentRequest;
import com.min.meow.comment.entity.LostCatPostComment;
import com.min.meow.comment.repository.LostCatPostCommentRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LostCatPostCommentService {

    private final LostCatRepository lostCatRepository;
    private final LostCatPostCommentRepository lostCatPostCommentRepository;

    @Transactional
    public RegisterCommentDto registerComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        LostCatPostComment lostCatPostComment = LostCatPostComment.convertToEntity(registerCommentRequest, lostCatPost);
        lostCatPostCommentRepository.save(lostCatPostComment);

        return RegisterCommentDto.convertToDto(lostCatPostComment);
    }
}
