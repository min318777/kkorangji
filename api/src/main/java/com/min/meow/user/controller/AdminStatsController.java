package com.min.meow.user.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.user.service.DauService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "관리자 - 통계", description = "관리자 전용 서비스 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasAuthority('user:manage')")
public class AdminStatsController {

    private final DauService dauService;

    @Operation(summary = "DAU 조회", description = "특정 날짜의 일간 활성 사용자 수를 조회합니다. date 미입력 시 오늘.")
    @GetMapping("/dau")
    public ResponseEntity<ApiResponse<Long>> getDau(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date != null ? date : LocalDate.now();
        Long dau = dauService.getCount(target);
        return ResponseEntity.ok(ApiResponse.success(target + " DAU 조회 성공", dau));
    }
}
