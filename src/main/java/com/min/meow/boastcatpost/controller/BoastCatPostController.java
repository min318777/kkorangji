package com.min.meow.boastcatpost.controller;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.CreateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.UpdateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.boastcatpost.service.BoastCatPostService;
import com.min.meow.global.ResponseDto;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostService boastCatPostService;

    // 모든 글 조회
    @GetMapping
    public ResponseEntity<?> getAllBoastCatPost(@RequestParam (defaultValue = "0") int page,
                                                @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        Page<BoastCatPostDto> posts = boastCatPostService.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", posts));
    }
    // 글 생성
    @PostMapping
    public ResponseEntity<?> createBoastCatPost(@RequestBody @Valid CreateBoastCatPostRequest createBoastCatPostRequest){

        CreateBoastCatPostDto post = boastCatPostService.createBoastCatPost(createBoastCatPostRequest);
        return ResponseEntity.ok(new ResponseDto<>(true, "글 생성 성공", post));
    }

    // 글 수정
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<?> updateBoastCatPost(@RequestBody @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest, @PathVariable Long boastCatPostId){

        UpdateBoastCatPostDto post = boastCatPostService.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "글 수정 성공", post));
    }

    // 글 삭제
    @DeleteMapping("/{boastCatPostId}")
    public ResponseEntity<?> deleteBoastCatPost(@PathVariable Long boastCatPostId){

        boastCatPostService.deleteBoastCatPost(boastCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "글 삭제 성공", null));
    }
}
