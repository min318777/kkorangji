package com.min.meow.boastcatpost.service;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.CreateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.UpdateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.boastcatpost.entity.BoastCatPost;
import com.min.meow.boastcatpost.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;

    // 모든 글 조회
    public Page<BoastCatPostDto> getAllBoastCatPosts(Pageable pageable){

        return boastCatPostRepository.findAll(pageable).map(BoastCatPostDto::convertToDto);
    }

    // 글 작성
    @Transactional
    public CreateBoastCatPostDto createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId){

        User writer = userRepository.findByLoginId(loginId);
        BoastCatPost boastCatPost = BoastCatPost.convertToEntity(createBoastCatPostRequest, writer);

        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostDto.convertToDto(boastCatPost);
    }

    // 글 수정
    @Transactional
    public UpdateBoastCatPostDto updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, String loginId){

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        if(!boastCatPost.getUser().getLoginId().equals(loginId)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPost.update(updateBoastCatPostRequest);

        return UpdateBoastCatPostDto.convertToDto(boastCatPost);
    }

    // 글 삭제
    @Transactional
    public void deleteBoastCatPost(Long boastCatPostId, String loginId){

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        if(!boastCatPost.getUser().getLoginId().equals(loginId)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
        /*
        if(!boastCatPostRepository.existsById(boastCatPostId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
         */
    }
}
