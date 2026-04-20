package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.checkout.CheckoutPreviewRequest;
import org.example.common.dto.checkout.CheckoutPreviewResponse;
import org.example.common.entity.AppUser;
import org.example.common.entity.CartItem;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final AppUserRepository appUserRepository;
    private final CartItemRepository cartItemRepository;
    private final CheckoutPricingService checkoutPricingService;

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Integer userId, CheckoutPreviewRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByCartUserId(user.getId());
        CheckoutPricingService.PricingResult pricing = checkoutPricingService.calculate(
                user.getId(),
                cartItems,
                request != null ? request.getCouponCode() : null,
                request != null ? request.getInstallmentCount() : null
        );

        String normalizedCouponCode = pricing.coupon() != null ? pricing.coupon().getCode() : null;

        return new CheckoutPreviewResponse(
                pricing.itemCount(),
                pricing.installmentCount(),
                normalizedCouponCode,
                pricing.subtotal(),
                pricing.discountTotal(),
                pricing.shippingFee(),
                pricing.grandTotal()
        );
    }
}