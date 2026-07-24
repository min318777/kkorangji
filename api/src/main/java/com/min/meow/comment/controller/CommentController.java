package com.min.meow.comment.controller;


import com.min.meow.common.PageResponse;
import com.min.meow.common.PostType;
import com.min.meow.common.PrincipalUser;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글", description = "게시글 댓글 CRUD API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ==================== 댓글 조회/작성 (자랑글·실종글 공통) ====================

    @Operation(summary = "댓글 조회 (페이징)",
            description = "게시글 댓글을 조회합니다. postType: boast-cat | lost-cat. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/api/meow/{postType}/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getComments(
            @Parameter(description = "게시글 타입 (boast-cat | lost-cat)", example = "boast-cat")
            @PathVariable String postType,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetCommentResponse> comments = commentService.getComments(postId, resolvePostType(postType), pageable);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Operation(summary = "댓글 작성",
            description = "게시글에 댓글을 작성합니다. postType: boast-cat | lost-cat. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:write')")
    @PostMapping("/api/meow/{postType}/{postId}/comments")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerComment(
            @RequestBody @Valid RegisterCommentRequest request,
            @Parameter(description = "게시글 타입 (boast-cat | lost-cat)", example = "boast-cat")
            @PathVariable String postType,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        RegisterCommentResponse response = commentService.registerComment(request, postId, resolvePostType(postType), user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글 작성 성공", response));
    }

    // ==================== 공통 댓글 관리 API ====================

    @Operation(summary = "댓글 수정",
            description = "댓글을 수정합니다. 게시글 타입에 관계없이 댓글 ID로 수정합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:write')")
    @PutMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(
            @RequestBody @Valid UpdateCommentRequest updateCommentRequest,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentService.updateComment(updateCommentRequest, commentId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", updateCommentResponse));
    }

    @Operation(summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 게시글 타입에 관계없이 댓글 ID로 삭제합니다. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        commentService.deleteComment(commentId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    // URL의 postType 문자열을 PostType enum으로 변환
    private PostType resolvePostType(String postType) {
        return switch (postType) {
            case "boast-cat" -> PostType.BOAST;
            case "lost-cat"  -> PostType.LOST;
            default          -> throw new CustomException(ErrorCode.NOT_FOUND_POST);
        };
    }
}
