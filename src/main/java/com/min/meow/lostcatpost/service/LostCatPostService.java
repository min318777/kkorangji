package com.min.meow.lostcatpost.service;


import com.min.meow.lostcatpost.domain.dto.LostCatPostDto;
import com.min.meow.lostcatpost.domain.entity.LostCatPost;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LostCatPostService {

    private final LostCatRepository lostCatRepository;

    public Page<LostCatPostDto> getAllLostCatPosts(Pageable pageable){

        return lostCatRepository.findAll(pageable).map(LostCatPostDto::convertToDto);
    }

    public LostCatPostDto createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest){

        LostCatPost lostCatPost = LostCatPost.convertToEntity(createLostCatPostRequest);
        lostCatRepository.save(lostCatPost);

        return LostCatPostDto.convertToDto(lostCatPost);
    }

}
