package com.min.meow.boastcatpost.service;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.CreateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.entity.BoastCatPost;
import com.min.meow.boastcatpost.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;

    // 모든 글 조회
    public Page<BoastCatPostDto> getAllBoastCatPosts(Pageable pageable){

        return boastCatPostRepository.findAll(pageable).map(BoastCatPostDto::convertToDto);
    }

    // 글 작성
    @Transactional
    public CreateBoastCatPostDto createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest){

        BoastCatPost boastCatPost = BoastCatPost.convertToEntity(createBoastCatPostRequest);

        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostDto.convertToDto(boastCatPost);
    }

    // 글 삭제
    @Transactional
    public void deleteBoastCatPost(Long boastCatPostId){

        if(!boastCatPostRepository.existsById(boastCatPostId)){
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
    }
}
