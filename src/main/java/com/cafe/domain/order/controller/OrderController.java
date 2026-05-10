package com.cafe.domain.order.controller;

import com.cafe.common.dto.ApiResponse;
import com.cafe.domain.order.dto.*;
import com.cafe.domain.order.service.OrderService;
import com.cafe.infrastructure.security.annotation.LoginUser;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.createOrder(request, loginUser)));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderGetResponse>> getOrder(
            @PathVariable String orderNumber,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(orderNumber, loginUser)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderSliceResponse>> getMyOrders(
            @Valid @ModelAttribute OrderSearchRequest request,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(loginUser, request.toPageable())));
    }

    @PatchMapping("/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponse>> cancelOrder(
            @PathVariable String orderNumber,
            @LoginUser LoginUserInfoDto loginUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(orderNumber, loginUser)));
    }
}
