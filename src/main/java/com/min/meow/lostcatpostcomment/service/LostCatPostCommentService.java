package com.min.meow.lostcatpostcomment.service;


import com.min.meow.lostcatpostcomment.domain.dto.RegisterLostCatPostCommentDto;
import com.min.meow.lostcatpostcomment.domain.dto.UpdateLostCatPostCommentDto;
import com.min.meow.lostcatpostcomment.domain.request.RegisterLostCatPostCommentRequest;
import com.min.meow.lostcatpostcomment.domain.request.UpdateLostCatPostCommentRequest;
import com.min.meow.lostcatpostcomment.entity.LostCatPostComment;
import com.min.meow.lostcatpostcomment.repository.LostCatPostCommentRepository;
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

    // 댓글 작성
    @Transactional
    public RegisterLostCatPostCommentDto registerLostCatPostComment(RegisterLostCatPostCommentRequest registerLostCatPostCommentRequest, Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        LostCatPostComment lostCatPostComment = LostCatPostComment.convertToEntity(registerLostCatPostCommentRequest, lostCatPost);
        lostCatPostCommentRepository.save(lostCatPostComment);

        return RegisterLostCatPostCommentDto.convertToDto(lostCatPostComment);
    }

    // 댓글 수정
    @Transactional
    public UpdateLostCatPostCommentDto updateLostCatPostComment(UpdateLostCatPostCommentRequest updateLostCatPostCommentRequest, Long lostCatPostCommentId){
        LostCatPostComment lostCatPostComment = lostCatPostCommentRepository.findById(lostCatPostCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        lostCatPostComment.update(updateLostCatPostCommentRequest);

        return UpdateLostCatPostCommentDto.convertToDto(lostCatPostComment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteLostCatPostComment(Long lostCatPostCommentId){

        if(!lostCatPostCommentRepository.existsById(lostCatPostCommentId)){
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        lostCatPostCommentRepository.deleteById(lostCatPostCommentId);
    }
}
