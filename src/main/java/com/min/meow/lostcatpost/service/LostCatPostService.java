package com.min.meow.lostcatpost.service;


import com.min.meow.lostcatpost.domain.LostCatPostEntity;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LostCatPostService {

    private final LostCatRepository lostCatRepository;

    public Page<LostCatPostEntity> getAllLostCatPosts(Pageable pageable){



        return lostCatRepository.findAll(pageable);
    }
}
