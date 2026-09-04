package com.min.meow.comment.controller;


import com.min.meow.common.PageResponse;
import com.min.meow.common.PostType;
import com.min.meow.common.PrincipalUser;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.SecurityUtil;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @Operation(summary = "댓글 조회 (페이징)",
            description = "게시글 댓글을 조회합니다. postType: boast-cat | lost-cat. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/api/meow/{postType}/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getComments(
            @Parameter(description = "게시글 타입 (boast-cat | lost-cat)", example = "boast-cat")
            @PathVariable String postType,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<GetCommentResponse> comments = commentService.getComments(postId, resolvePostType(postType), pageable);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Operation(summary = "댓글 작성",
            description = "게시글에 댓글을 작성합니다. postType: boast-cat | lost-cat. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:create')")
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

    @Operation(summary = "댓글 수정",
            description = "댓글을 수정합니다. 게시글 타입에 관계없이 댓글 ID로 수정합니다. 본인 댓글만 수정 가능합니다(관리자는 타인 댓글도 수정 가능). 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(
            @RequestBody @Valid UpdateCommentRequest updateCommentRequest,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentService.updateComment(updateCommentRequest, commentId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", updateCommentResponse));
    }

    @Operation(summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 게시글 타입에 관계없이 댓글 ID로 삭제합니다. 본인 댓글만 삭제 가능합니다(관리자는 타인 댓글도 삭제 가능). 인증 필요.")
    @PreAuthorize("hasAuthority('comment:delete')")
    @DeleteMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        boolean hasDeleteAuthority = SecurityUtil.hasAuthority("comment:delete:any");
        commentService.deleteComment(commentId, user.getUserId(), hasDeleteAuthority);
        return ResponseEntity.noContent().build();
    }

    // URL의 postType 문자열을 PostType enum으로 변환
    private PostType resolvePostType(String postType) {
        return switch (postType) {
            case "boast-cat" -> PostType.BOAST;
            case "lost-cat"  -> PostType.LOST;
            default          -> throw new CustomException(ErrorCode.INVALID_POST_TYPE);
        };
    }
}
