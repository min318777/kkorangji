package com.min.meow.boastcatpost.controller;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.service.BoastCatPostService;
import com.min.meow.global.ResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostService boastCatPostService;

    @GetMapping
    public ResponseEntity<?> getAllBoastCatPost(@RequestParam (defaultValue = "0") int page,
                                                @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        Page<BoastCatPostDto> posts = boastCatPostService.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", posts));
    }
}
