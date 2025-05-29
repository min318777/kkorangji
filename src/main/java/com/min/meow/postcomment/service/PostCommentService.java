package com.min.meow.postcomment.service;


import com.min.meow.notice.NoticeDto;
import com.min.meow.kafka.producer.KafkaMessageProducer;
import com.min.meow.postcomment.domain.dto.RegisterPostCommentDto;
import com.min.meow.postcomment.domain.dto.UpdatePostCommentDto;
import com.min.meow.postcomment.domain.request.RegisterPostCommentRequest;
import com.min.meow.postcomment.domain.request.UpdatePostCommentRequest;
import com.min.meow.postcomment.entity.PostComment;
import com.min.meow.postcomment.repository.PostCommentRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PostCommentService {

    private final LostCatRepository lostCatRepository;
    private final PostCommentRepository postCommentRepository;
    private final KafkaMessageProducer kafkaMessageProducer;

    // 댓글 작성
    @Transactional
    public RegisterPostCommentDto registerLostCatPostComment(RegisterPostCommentRequest registerPostCommentRequest, Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        PostComment postComment = PostComment.convertToEntity(registerPostCommentRequest, lostCatPost);
        lostCatPost.getPostComments().add(postComment);
        postCommentRepository.save(postComment);

        NoticeDto noticeDto = NoticeDto.builder()
                .commentId(postComment.getPostCommentId())
                .postId(lostCatPostId)
                .message("게시글에 댓글이 달렸습니다.")
                .receiverLoginId(lostCatPost.getUser().getLoginId())
                .createdAt(LocalDateTime.now())
                .build();
        produceTopic(noticeDto);
        return RegisterPostCommentDto.convertToDto(postComment);
    }

    // 댓글 수정
    @Transactional
    public UpdatePostCommentDto updateLostCatPostComment(UpdatePostCommentRequest updatePostCommentRequest, Long lostCatPostCommentId){
        PostComment postComment = postCommentRepository.findById(lostCatPostCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        postComment.update(updatePostCommentRequest);

        return UpdatePostCommentDto.convertToDto(postComment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteLostCatPostComment(Long lostCatPostCommentId){

        if(!postCommentRepository.existsById(lostCatPostCommentId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

        postCommentRepository.deleteById(lostCatPostCommentId);
    }

    public void produceTopic(NoticeDto noticeDto){

        kafkaMessageProducer.send("lost-cat-topic", noticeDto);
    }
}
