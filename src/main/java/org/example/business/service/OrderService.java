package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.checkout.OrderCreateRequest;
import org.example.common.dto.checkout.OrderDetailResponse;
import org.example.common.dto.checkout.OrderItemResponse;
import org.example.common.dto.checkout.OrderSummaryResponse;
import org.example.common.dto.checkout.PaymentRequest;
import org.example.common.dto.checkout.PaymentResultResponse;
import org.example.common.dto.checkout.ShippingAddressRequest;
import org.example.common.entity.AppUser;
import org.example.common.entity.CartItem;
import org.example.common.entity.Order;
import org.example.common.entity.OrderItem;
import org.example.common.entity.OrderStatus;
import org.example.common.entity.PaymentAttempt;
import org.example.common.entity.PaymentProvider;
import org.example.common.entity.PaymentStatus;
import org.example.common.entity.ShippingAddress;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.CartItemRepository;
import org.example.data.repository.OrderRepository;
import org.example.data.repository.PaymentAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final AppUserRepository appUserRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final CheckoutPricingService checkoutPricingService;
    private final CouponService couponService;
    private final AuditLogService auditLogService;

    @Transactional
    public OrderDetailResponse createFromCart(Integer userId, OrderCreateRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByCartUserId(user.getId());
        CheckoutPricingService.PricingResult pricing = checkoutPricingService.calculate(
                user.getId(),
                cartItems,
                request != null ? request.getCouponCode() : null,
                request != null ? request.getInstallmentCount() : null
        );

        if (request == null || request.getShippingAddress() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setInstallmentCount(pricing.installmentCount());
        order.setSubtotal(pricing.subtotal());
        order.setDiscountTotal(pricing.discountTotal());
        order.setShippingFee(pricing.shippingFee());
        order.setGrandTotal(pricing.grandTotal());
        order.setCouponCode(pricing.coupon() != null ? pricing.coupon().getCode() : null);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setProductName(cartItem.getProduct().getName());
            item.setProductSku(buildSku(cartItem));
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(cartItem.getProduct().getPrice());
            item.setLineTotal(cartItem.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(item);
        }
        order.setItems(orderItems);

        ShippingAddress shippingAddress = mapShippingAddress(request.getShippingAddress(), order);
        order.setShippingAddress(shippingAddress);

        Order saved = orderRepository.save(order);

        if (pricing.resolvedCoupon() != null) {
            couponService.consumeResolvedCoupon(pricing.resolvedCoupon());
        }

        PaymentAttempt initialAttempt = new PaymentAttempt();
        initialAttempt.setOrder(saved);
        initialAttempt.setProvider(resolveProvider(request.getPaymentProvider()));
        initialAttempt.setStatus(PaymentStatus.PENDING);
        initialAttempt.setTransactionId(null);
        initialAttempt.setErrorCode(null);
        initialAttempt.setErrorMessage("Order created, payment not started.");
        paymentAttemptRepository.save(initialAttempt);

        cartItemRepository.deleteAll(cartItems);

        auditLogService.log("ORDER_CREATE", "ORDER", saved.getId(), "Order created from cart: " + saved.getOrderNumber());
        return toDetailResponse(saved);
    }

    @Transactional
    public PaymentResultResponse pay(Integer userId, Integer orderId, PaymentRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        PaymentProvider provider = resolveProvider(request != null ? request.getPaymentProvider() : null);

        // Stock check before payment: prevents oversell on concurrent purchases.
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            int currentStock = item.getProduct().getStockQuantity() == null ? 0 : item.getProduct().getStockQuantity();
            int requested = item.getQuantity() == null ? 0 : item.getQuantity();
            if (requested > currentStock) {
                throw new AppException(ErrorCode.NOT_ENOUGH_STOCK, item.getProductName());
            }
        }

        // Payment success -> decrease product stocks.
        for (OrderItem item : order.getItems()) {
            int currentStock = item.getProduct().getStockQuantity() == null ? 0 : item.getProduct().getStockQuantity();
            int requested = item.getQuantity() == null ? 0 : item.getQuantity();
            item.getProduct().setStockQuantity(currentStock - requested);
        }

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrder(order);
        attempt.setProvider(provider);
        attempt.setStatus(PaymentStatus.SUCCESS);
        attempt.setTransactionId(generateTransactionId(provider));
        attempt.setErrorCode(null);
        attempt.setErrorMessage(null);
        paymentAttemptRepository.save(attempt);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        auditLogService.log("ORDER_PAY", "ORDER", order.getId(), "Order paid: " + order.getOrderNumber());

        return new PaymentResultResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                attempt.getTransactionId(),
                "Odeme basarili."
        );
    }

    @Transactional
    public PaymentResultResponse retryPayment(Integer userId, Integer orderId, PaymentRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        return pay(userId, orderId, request);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMyOrders(Integer userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus().name(),
                        order.getItems() != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0,
                        order.getGrandTotal(),
                        order.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrderDetail(Integer userId, Integer orderId) {
        Order order = orderRepository.findDetailedByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toDetailResponse(order);
    }
    @Transactional
    public OrderDetailResponse cancel(Integer userId, Integer orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toDetailResponse(order);
        }

        // If a paid order is cancelled, return stocks back.
        if (order.getStatus() == OrderStatus.PAID) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) {
                    continue;
                }
                int currentStock = item.getProduct().getStockQuantity() == null ? 0 : item.getProduct().getStockQuantity();
                int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                item.getProduct().setStockQuantity(currentStock + qty);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        auditLogService.log("ORDER_CANCEL", "ORDER", saved.getId(), "Order cancelled: " + saved.getOrderNumber());
        return toDetailResponse(saved);
    }
    private OrderDetailResponse toDetailResponse(Order order) {
        ShippingAddressRequest shippingResponse = new ShippingAddressRequest();
        if (order.getShippingAddress() != null) {
            shippingResponse.setFullName(order.getShippingAddress().getFullName());
            shippingResponse.setPhoneNumber(order.getShippingAddress().getPhoneNumber());
            shippingResponse.setCity(order.getShippingAddress().getCity());
            shippingResponse.setDistrict(order.getShippingAddress().getDistrict());
            shippingResponse.setAddressLine(order.getShippingAddress().getAddressLine());
            shippingResponse.setPostalCode(order.getShippingAddress().getPostalCode());
            shippingResponse.setCountry(order.getShippingAddress().getCountry());
        }

        List<OrderItemResponse> itemResponses = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getProductName(),
                        item.getProductSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getInstallmentCount(),
                order.getCouponCode(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getShippingFee(),
                order.getGrandTotal(),
                shippingResponse,
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private ShippingAddress mapShippingAddress(ShippingAddressRequest request, Order order) {
        ShippingAddress address = new ShippingAddress();
        address.setOrder(order);
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setAddressLine(request.getAddressLine());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        return address;
    }

    private PaymentProvider resolveProvider(PaymentProvider provider) {
        return provider == null ? PaymentProvider.MOCK : provider;
    }

    private String buildSku(CartItem cartItem) {
        String skuBase = cartItem.getProduct() != null && cartItem.getProduct().getId() != null
                ? "PRD-" + cartItem.getProduct().getId()
                : "PRD-NA";
        return skuBase.toUpperCase(Locale.ROOT);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "ORD-" + date + "-" + suffix;
    }

    private String generateTransactionId(PaymentProvider provider) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return provider.name() + "-" + token;
    }
}



