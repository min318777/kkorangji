package com.min.meow.boastcatpost.service;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.repository.BoastCatPostRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;

    public Page<BoastCatPostDto> getAllBoastCatPosts(Pageable pageable){

        return boastCatPostRepository.findAll(pageable).map(BoastCatPostDto::convertToDto);
    }
}
