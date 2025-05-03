package com.min.meow.lostcatpost.controller;


import com.min.meow.global.ResponseDto;
import com.min.meow.lostcatpost.domain.dto.CreateLostCatPostDto;
import com.min.meow.lostcatpost.domain.dto.UpdateLostCatPostDto;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.service.LostCatPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat")
public class LostCatPostController {

    private final LostCatPostService lostCatPostService;

    // 모든 게시물 조회
    @GetMapping
    public ResponseEntity<?> getAllLostCatPosts(@RequestParam (defaultValue = "0") int page,
                                                @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UpdateLostCatPostDto> posts = lostCatPostService.getAllLostCatPosts(pageable);

        return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", posts));
    }

    // 글 생성
    @PostMapping("/create")
    public ResponseEntity<?> createLostCatPost(@RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
                                               BindingResult bindingResult){

        CreateLostCatPostDto post = lostCatPostService.createLostCatPost(createLostCatPostRequest);

        return ResponseEntity.ok(new ResponseDto<>(true, "글 생성 성공", post));
    }

    // 글 수정
    @PutMapping("/update/{lostCatPostId}")
    public ResponseEntity<?> updateLostCatPost(@PathVariable Long lostCatPostId,
                                               @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
                                               BindingResult bindingResult){

        UpdateLostCatPostDto post = lostCatPostService.updateLostCatPost(lostCatPostId, updateLostCatPostRequest);
        return ResponseEntity.ok(new ResponseDto<>(true, "글 수정 성공", post));

    }

    // 글 삭제
    @DeleteMapping("/delete/{lostCatPostId}")
    public ResponseEntity<?> deleteLostCatPost(@PathVariable Long lostCatPostId){

        lostCatPostService.deleteLostCatPost(lostCatPostId);

        return ResponseEntity.ok(new ResponseDto<>(true, "글 삭제 성공", null));
    }


}
