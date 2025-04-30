package com.min.meow.lostcatpost.service;


import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.lostcatpost.domain.dto.LostCatPostDto;
import com.min.meow.lostcatpost.domain.entity.LostCatPost;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LostCatPostService {

    private final LostCatRepository lostCatRepository;

    // 글 전체 조회
    public Page<LostCatPostDto> getAllLostCatPosts(Pageable pageable){

        return lostCatRepository.findAll(pageable).map(LostCatPostDto::convertToDto);
    }

    // 글 생성
    public LostCatPostDto createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest){

        LostCatPost lostCatPost = LostCatPost.convertToEntity(createLostCatPostRequest);
        lostCatRepository.save(lostCatPost);

        return LostCatPostDto.convertToDto(lostCatPost);
    }
    
    // 글 수정
    @Transactional
    public LostCatPostDto updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest){

        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        lostCatPost.update(updateLostCatPostRequest);
        return LostCatPostDto.convertToDto(lostCatPost);
    }

}
