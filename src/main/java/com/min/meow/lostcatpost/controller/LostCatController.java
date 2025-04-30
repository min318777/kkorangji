package com.min.meow.lostcatpost.controller;


import com.min.meow.common.ResponseDto;
import com.min.meow.lostcatpost.domain.dto.LostCatPostDto;
import com.min.meow.lostcatpost.domain.entity.LostCatPost;
import com.min.meow.lostcatpost.service.LostCatPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat")
public class LostCatController {

    private final LostCatPostService lostCatPostService;

    // 모든 게시물 조회
    @GetMapping
    public ResponseEntity<?> getAllLostCatPosts(
            @RequestParam (defaultValue = "0") int page,
            @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LostCatPostDto> posts = lostCatPostService.getAllLostCatPosts(pageable);

        return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", posts));
    }

}
