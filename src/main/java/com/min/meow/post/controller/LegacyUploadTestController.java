package com.min.meow.post.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.PrincipalUser;
import com.min.meow.config.S3Uploader;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.service.BoastCatPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 서버 경유 업로드(Before) vs Presigned URL(After) 응답 시간 비교 측정 전용 컨트롤러.
 * local/prod 프로필에서만 활성화된다.
 * prod에서의 활성화는 실사용자 없이 배포만 해둔 테스트 EC2에서 t3.small(2GB) 등
 * 실제 배포 환경 리소스 제약 하에 성능을 측정하기 위한 것으로, 실사용자가 있는
 * 서비스 환경에 배포할 경우 반드시 이 컨트롤러를 제거하거나 프로필 조건을 되돌릴 것.
 * S3Uploader(현재 @Deprecated)를 그대로 재사용해 동영상을 서버가 직접 받아 S3로 재전송한다.
 */
@Tag(name = "[TEST] 서버 경유 업로드 비교", description = "Presigned 방식과의 응답 시간 비교 측정 전용 — local/prod(테스트 EC2)에서만 활성화")
@Slf4j
@Profile({"local", "prod"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/boast-posts")
public class LegacyUploadTestController {

    private final S3Uploader s3Uploader;
    private final BoastCatPostService boastCatPostService;

    @Operation(summary = "[TEST] 서버 경유 동영상 업로드 후 자랑글 작성",
            description = "동영상을 서버가 MultipartFile로 직접 받아 S3에 재전송한 뒤 자랑글을 작성한다. "
                    + "Presigned 방식과의 응답 시간 비교 측정 전용 — 실서비스 미반영.")
    @PostMapping("/legacy-upload")
    public ResponseEntity<ApiResponse<CreateBoastCatPostResponse>> legacyUpload(
            @RequestParam("video") MultipartFile video,
            @RequestParam("title") String title,
            @RequestParam(value = "content", required = false, defaultValue = "") String content,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        String videoUrl = s3Uploader.uploadFile(video);
        log.info("[legacy-upload] 서버 경유 업로드 완료 - size: {} bytes, url: {}", video.getSize(), videoUrl);

        CreateBoastCatPostRequest request = CreateBoastCatPostRequest.builder()
                .title(title)
                .content(content)
                .build();

        CreateBoastCatPostResponse response = boastCatPostService.createBoastCatPost(request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("서버 경유 업로드 테스트 완료", response));
    }
}
