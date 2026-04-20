package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.CartItem;
import org.example.common.entity.Coupon;
import org.example.common.entity.CouponDiscountType;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutPricingService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000.00");
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("99.90");

    private final CouponService couponService;

    public PricingResult calculate(Integer userId, List<CartItem> cartItems, String couponCode, Integer installmentCount) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        int safeInstallment = normalizeInstallment(installmentCount);
        BigDecimal subtotal = calculateSubtotal(cartItems);
        BigDecimal shippingFee = resolveShippingFee(subtotal);
        CouponService.ResolvedCoupon resolvedCoupon = couponService.resolveCouponForUser(userId, couponCode);
        Coupon coupon = resolvedCoupon != null ? resolvedCoupon.coupon() : null;
        BigDecimal discount = calculateDiscount(subtotal, coupon);
        BigDecimal grandTotal = subtotal.subtract(discount).add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        return new PricingResult(
                cartItems.stream().mapToInt(CartItem::getQuantity).sum(),
                safeInstallment,
                subtotal,
                discount,
                shippingFee,
                grandTotal,
                resolvedCoupon
        );
    }

    public int normalizeInstallment(Integer installmentCount) {
        if (installmentCount == null) {
            return 1;
        }
        if (installmentCount < 1 || installmentCount > 12) {
            throw new AppException(ErrorCode.INVALID_INSTALLMENT_COUNT);
        }
        return installmentCount;
    }

    public BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveShippingFee(BigDecimal subtotal) {
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return DEFAULT_SHIPPING_FEE.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, Coupon coupon) {
        if (coupon == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new AppException(ErrorCode.COUPON_MIN_ORDER_NOT_MET);
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    public static final class PricingResult {
        private final Integer itemCount;
        private final Integer installmentCount;
        private final BigDecimal subtotal;
        private final BigDecimal discountTotal;
        private final BigDecimal shippingFee;
        private final BigDecimal grandTotal;
        private final CouponService.ResolvedCoupon resolvedCoupon;

        public PricingResult(Integer itemCount,
                             Integer installmentCount,
                             BigDecimal subtotal,
                             BigDecimal discountTotal,
                             BigDecimal shippingFee,
                             BigDecimal grandTotal,
                             CouponService.ResolvedCoupon resolvedCoupon) {
            this.itemCount = itemCount;
            this.installmentCount = installmentCount;
            this.subtotal = subtotal;
            this.discountTotal = discountTotal;
            this.shippingFee = shippingFee;
            this.grandTotal = grandTotal;
            this.resolvedCoupon = resolvedCoupon;
        }

        public Integer itemCount() {
            return itemCount;
        }

        public Integer installmentCount() {
            return installmentCount;
        }

        public BigDecimal subtotal() {
            return subtotal;
        }

        public BigDecimal discountTotal() {
            return discountTotal;
        }

        public BigDecimal shippingFee() {
            return shippingFee;
        }

        public BigDecimal grandTotal() {
            return grandTotal;
        }

        public CouponService.ResolvedCoupon resolvedCoupon() {
            return resolvedCoupon;
        }

        public Coupon coupon() {
            return resolvedCoupon != null ? resolvedCoupon.coupon() : null;
        }
    }
}