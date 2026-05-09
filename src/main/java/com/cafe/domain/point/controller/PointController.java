package com.cafe.domain.point.controller;

import com.cafe.common.dto.ApiResponse;
import com.cafe.domain.point.dto.PointChargeRequest;
import com.cafe.domain.point.dto.PointChargeResponse;
import com.cafe.domain.point.service.PointService;
import com.cafe.infrastructure.security.annotation.LoginUser;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {
    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @Valid @RequestBody PointChargeRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(pointService.charge(request, loginUser)));
    }
}
