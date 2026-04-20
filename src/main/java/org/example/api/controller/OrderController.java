package org.example.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.business.service.OrderService;
import org.example.common.dto.checkout.OrderCreateRequest;
import org.example.common.dto.checkout.OrderDetailResponse;
import org.example.common.dto.checkout.OrderSummaryResponse;
import org.example.common.dto.checkout.PaymentRequest;
import org.example.common.dto.checkout.PaymentResultResponse;
import org.example.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Only User")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    @PostMapping
    @Operation(summary = "Create Order", description = "Create order from current cart")
    public ResponseEntity<OrderDetailResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.createFromCart(userId, request));
    }

    @PreAuthorize("hasAuthority('ORDER_LIST_SELF')")
    @GetMapping({"", "/my", "/list"})
    @Operation(summary = "List My Orders", description = "List authenticated user's orders")
    public ResponseEntity<List<OrderSummaryResponse>> listMyOrders() {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.listMyOrders(userId));
    }

    @PreAuthorize("hasAuthority('ORDER_DETAIL_SELF')")
    @GetMapping("/{orderId}")
    @Operation(summary = "My Order Detail", description = "Get authenticated user's order detail")
    public ResponseEntity<OrderDetailResponse> detail(@PathVariable Integer orderId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.getMyOrderDetail(userId, orderId));
    }

    @PreAuthorize("hasAuthority('ORDER_PAY')")
    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Pay Order", description = "Pay a pending order")
    public ResponseEntity<PaymentResultResponse> pay(@PathVariable Integer orderId,
                                                     @RequestBody(required = false) PaymentRequest request) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.pay(userId, orderId, request));
    }

    @PreAuthorize("hasAuthority('ORDER_RETRY_PAYMENT')")
    @PostMapping("/{orderId}/retry-payment")
    @Operation(summary = "Retry Payment", description = "Retry payment for a failed/pending order")
    public ResponseEntity<PaymentResultResponse> retryPayment(@PathVariable Integer orderId,
                                                              @RequestBody(required = false) PaymentRequest request) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.retryPayment(userId, orderId, request));
    }
    @PreAuthorize("hasAuthority('ORDER_PAY')")
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel Order", description = "Cancel authenticated user's order")
    public ResponseEntity<OrderDetailResponse> cancel(@PathVariable Integer orderId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.cancel(userId, orderId));
    }

    @PreAuthorize("hasAuthority('ORDER_PAY')")
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel Order (Fallback)", description = "Cancel authenticated user's order (POST fallback)")
    public ResponseEntity<OrderDetailResponse> cancelWithPost(@PathVariable Integer orderId) {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.cancel(userId, orderId));
    }
}


