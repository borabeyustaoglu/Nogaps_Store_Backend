package org.example.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.business.service.CouponService;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.coupon.CouponAssignRequest;
import org.example.common.dto.coupon.CouponCreateRequest;
import org.example.common.dto.coupon.CouponManageItemResponse;
import org.example.common.dto.coupon.MyCouponItemResponse;
import org.example.common.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @GetMapping({"", "/list", "/all"})
    public ResponseEntity<List<CouponManageItemResponse>> listCouponsForManager() {
        return ResponseEntity.ok(couponService.listCouponsForManager());
    }

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @PostMapping
    public ResponseEntity<CouponManageItemResponse> createCoupon(@RequestBody CouponCreateRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(
                request,
                securityUtils.getCurrentUsernameOrSystem(),
                securityUtils.getCurrentUserFullNameOrSystem(),
                securityUtils.getCurrentRoleOrSystem()
        ));
    }

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @PostMapping("/{couponId}/assign")
    public ResponseEntity<CouponManageItemResponse> assignCoupon(@PathVariable Integer couponId,
                                                                  @RequestBody CouponAssignRequest request) {
        return ResponseEntity.ok(couponService.assignCouponToUsers(
                couponId,
                request,
                securityUtils.getCurrentUsernameOrSystem(),
                securityUtils.getCurrentUserFullNameOrSystem(),
                securityUtils.getCurrentRoleOrSystem()
        ));
    }

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @PostMapping("/{couponId}/deactivate")
    public ResponseEntity<CouponManageItemResponse> deactivateCoupon(@PathVariable Integer couponId) {
        return ResponseEntity.ok(couponService.deactivateCoupon(
                couponId,
                securityUtils.getCurrentUsernameOrSystem(),
                securityUtils.getCurrentUserFullNameOrSystem(),
                securityUtils.getCurrentRoleOrSystem()
        ));
    }

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @PostMapping("/{couponId}/reactivate")
    public ResponseEntity<CouponManageItemResponse> reactivateCoupon(@PathVariable Integer couponId) {
        return ResponseEntity.ok(couponService.reactivateCoupon(
                couponId,
                securityUtils.getCurrentUsernameOrSystem(),
                securityUtils.getCurrentUserFullNameOrSystem(),
                securityUtils.getCurrentRoleOrSystem()
        ));
    }

    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @DeleteMapping("/{couponId}")
    public ResponseEntity<MessageResponse> deleteCoupon(@PathVariable Integer couponId) {
        couponService.deleteCoupon(
                couponId,
                securityUtils.getCurrentUsernameOrSystem(),
                securityUtils.getCurrentUserFullNameOrSystem(),
                securityUtils.getCurrentRoleOrSystem()
        );
        return ResponseEntity.ok(new MessageResponse("Kupon silindi."));
    }

    @PreAuthorize("hasAuthority('COUPON_LIST_SELF')")
    @GetMapping("/my")
    public ResponseEntity<List<MyCouponItemResponse>> myCoupons() {
        Integer userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(couponService.listMyCoupons(userId));
    }
}