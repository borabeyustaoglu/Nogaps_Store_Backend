package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.coupon.CouponAssignRequest;
import org.example.common.dto.coupon.CouponCreateRequest;
import org.example.common.dto.coupon.CouponManageItemResponse;
import org.example.common.dto.coupon.MyCouponItemResponse;
import org.example.common.entity.AppUser;
import org.example.common.entity.Coupon;
import org.example.common.entity.CouponDiscountType;
import org.example.common.entity.UserCoupon;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.CouponRepository;
import org.example.data.repository.UserCouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final String WELCOME_COUPON_CODE = "HOSGELDIN5";

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<CouponManageItemResponse> listCouponsForManager() {
        return couponRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toManageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MyCouponItemResponse> listMyCoupons(Integer userId) {
        return userCouponRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toMyCouponResponse)
                .toList();
    }

    @Transactional
    public CouponManageItemResponse createCoupon(CouponCreateRequest request, String actorUsername, String actorFullName, String actorRole) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Kupon kodu zaten mevcut.");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType() == null ? CouponDiscountType.PERCENTAGE : request.getDiscountType());
        coupon.setDiscountValue(normalizePositiveMoney(request.getDiscountValue(), "discountValue"));
        coupon.setMinOrderAmount(request.getMinOrderAmount() == null ? BigDecimal.ZERO : request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        coupon.setUsageLimit(normalizeUsageLimit(request.getUsageLimit()));

        LocalDateTime startsAt = request.getStartsAt() == null ? LocalDateTime.now() : request.getStartsAt();
        LocalDateTime endsAt = resolveEndsAt(startsAt, request.getEndsAt(), request.getUnlimitedDuration());
        coupon.setStartsAt(startsAt);
        coupon.setEndsAt(endsAt);

        Coupon saved = couponRepository.save(coupon);

        List<Integer> userIds = request.getUserIds() == null ? List.of() : request.getUserIds();
        if (!userIds.isEmpty()) {
            LocalDateTime assignStarts = startsAt;
            LocalDateTime assignEnds = endsAt;
            assignCouponToUsersInternal(saved, userIds, request.getPerUserUsageLimit(), assignStarts, assignEnds, request.getUnlimitedDuration(), true);
        }

        auditLogService.logAs(actorUsername, actorFullName, actorRole, "CREATE", "COUPON", saved.getId(), "Coupon created: " + saved.getCode());

        return toManageResponse(saved);
    }

    @Transactional
    public CouponManageItemResponse assignCouponToUsers(Integer couponId, CouponAssignRequest request, String actorUsername, String actorFullName, String actorRole) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("En az bir kullanici secilmelidir.");
        }

        LocalDateTime startsAt = request.getStartsAt() != null
                ? request.getStartsAt()
                : (coupon.getStartsAt() != null ? coupon.getStartsAt() : LocalDateTime.now());

        Boolean unlimited = request.getUnlimitedDuration();
        LocalDateTime endsAt = resolveEndsAt(startsAt, request.getEndsAt() != null ? request.getEndsAt() : coupon.getEndsAt(), unlimited);

        assignCouponToUsersInternal(
                coupon,
                request.getUserIds(),
                request.getUsageLimit(),
                startsAt,
                endsAt,
                unlimited,
                request.getActive() == null ? true : request.getActive()
        );

        auditLogService.logAs(actorUsername, actorFullName, actorRole, "ASSIGN", "COUPON", coupon.getId(), "Coupon assigned to users: " + coupon.getCode());

        return toManageResponse(coupon);
    }
    @Transactional
    public CouponManageItemResponse deactivateCoupon(Integer couponId, String actorUsername, String actorFullName, String actorRole) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            return toManageResponse(coupon);
        }

        coupon.setActive(false);
        couponRepository.save(coupon);
        userCouponRepository.deactivateByCouponId(couponId);

        auditLogService.logAs(actorUsername, actorFullName, actorRole, "DEACTIVATE", "COUPON", couponId, "Coupon deactivated: " + coupon.getCode());

        return toManageResponse(coupon);
    }
    @Transactional
    public CouponManageItemResponse reactivateCoupon(Integer couponId, String actorUsername, String actorFullName, String actorRole) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        if (Boolean.TRUE.equals(coupon.getActive())) {
            return toManageResponse(coupon);
        }

        coupon.setActive(true);
        couponRepository.save(coupon);
        userCouponRepository.reactivateByCouponId(couponId);

        auditLogService.logAs(actorUsername, actorFullName, actorRole, "REACTIVATE", "COUPON", couponId, "Coupon reactivated: " + coupon.getCode());

        return toManageResponse(coupon);
    }

    @Transactional
    public void deleteCoupon(Integer couponId, String actorUsername, String actorFullName, String actorRole) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        String couponCode = coupon.getCode();
        userCouponRepository.deleteByCouponId(couponId);
        couponRepository.delete(coupon);

        auditLogService.logAs(actorUsername, actorFullName, actorRole, "DELETE", "COUPON", couponId, "Coupon deleted: " + couponCode);
    }

    @Transactional
    public void assignWelcomeCouponToUser(AppUser user) {
        if (user == null || user.getId() == null) {
            return;
        }

        Coupon welcomeCoupon = couponRepository.findByCodeIgnoreCase(WELCOME_COUPON_CODE)
                .orElseGet(this::createWelcomeCouponTemplate);

        userCouponRepository.findByUserIdAndCouponId(user.getId(), welcomeCoupon.getId())
                .ifPresentOrElse(existing -> {
                }, () -> {
                    UserCoupon assignment = new UserCoupon();
                    assignment.setUser(user);
                    assignment.setCoupon(welcomeCoupon);
                    assignment.setActive(true);
                    assignment.setUsageLimit(1);
                    assignment.setUsedCount(0);
                    assignment.setStartsAt(LocalDateTime.now());
                    assignment.setEndsAt(LocalDateTime.now().plusDays(7));
                    userCouponRepository.save(assignment);
                });
    }

    @Transactional
    public void backfillWelcomeCouponForAllUsers() {
        Coupon welcomeCoupon = couponRepository.findByCodeIgnoreCase(WELCOME_COUPON_CODE)
                .orElseGet(this::createWelcomeCouponTemplate);

        List<AppUser> users = appUserRepository.findAll();
        for (AppUser user : users) {
            userCouponRepository.findByUserIdAndCouponId(user.getId(), welcomeCoupon.getId())
                    .ifPresentOrElse(existing -> {
                    }, () -> {
                        UserCoupon assignment = new UserCoupon();
                        assignment.setUser(user);
                        assignment.setCoupon(welcomeCoupon);
                        assignment.setActive(true);
                        assignment.setUsageLimit(1);
                        assignment.setUsedCount(0);
                        assignment.setStartsAt(LocalDateTime.now());
                        assignment.setEndsAt(LocalDateTime.now().plusDays(7));
                        userCouponRepository.save(assignment);
                    });
        }
    }

    @Transactional(readOnly = true)
    public ResolvedCoupon resolveCouponForUser(Integer userId, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        validateCouponWindowAndLimit(coupon);

        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId()).orElse(null);
        if (userCoupon == null && userCouponRepository.existsByCouponId(coupon.getId())) {
            throw new AppException(ErrorCode.COUPON_NOT_ASSIGNED_TO_USER);
        }

        if (userCoupon != null) {
            validateUserCouponWindowAndLimit(userCoupon);
        }

        return new ResolvedCoupon(coupon, userCoupon);
    }

    @Transactional
    public void consumeResolvedCoupon(ResolvedCoupon resolvedCoupon) {
        if (resolvedCoupon == null || resolvedCoupon.coupon() == null) {
            return;
        }

        Coupon coupon = resolvedCoupon.coupon();
        Integer couponUsed = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        coupon.setUsedCount(couponUsed + 1);
        couponRepository.save(coupon);

        if (resolvedCoupon.userCoupon() != null) {
            UserCoupon userCoupon = resolvedCoupon.userCoupon();
            Integer assignmentUsed = userCoupon.getUsedCount() == null ? 0 : userCoupon.getUsedCount();
            userCoupon.setUsedCount(assignmentUsed + 1);
            userCouponRepository.save(userCoupon);
        }
    }

    private void assignCouponToUsersInternal(
            Coupon coupon,
            List<Integer> userIds,
            Integer usageLimit,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Boolean unlimitedDuration,
            boolean active
    ) {
        List<AppUser> users = appUserRepository.findAllById(new ArrayList<>(userIds));
        if (users.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime normalizedEndsAt = Boolean.TRUE.equals(unlimitedDuration) ? null : endsAt;
        Integer normalizedUsageLimit = normalizeUsageLimit(usageLimit);

        for (AppUser user : users) {
            UserCoupon assignment = userCouponRepository.findByUserIdAndCouponId(user.getId(), coupon.getId())
                    .orElseGet(UserCoupon::new);

            assignment.setUser(user);
            assignment.setCoupon(coupon);
            assignment.setActive(active);
            assignment.setUsageLimit(normalizedUsageLimit);
            assignment.setStartsAt(startsAt);
            assignment.setEndsAt(normalizedEndsAt);
            if (assignment.getUsedCount() == null) {
                assignment.setUsedCount(0);
            }
            userCouponRepository.save(assignment);
        }
    }

    private Coupon createWelcomeCouponTemplate() {
        Coupon coupon = new Coupon();
        coupon.setCode(WELCOME_COUPON_CODE);
        coupon.setDiscountType(CouponDiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("5"));
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(null);
        coupon.setActive(true);
        coupon.setUsageLimit(null);
        coupon.setStartsAt(LocalDateTime.now());
        coupon.setEndsAt(null);
        return couponRepository.save(coupon);
    }

    private String normalizeCode(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Kupon kodu zorunludur.");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizePositiveMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " sifirdan buyuk olmalidir.");
        }
        return value;
    }

    private Integer normalizeUsageLimit(Integer usageLimit) {
        if (usageLimit == null) {
            return null;
        }
        if (usageLimit <= 0) {
            throw new IllegalArgumentException("Kullanim limiti pozitif olmalidir.");
        }
        return usageLimit;
    }

    private LocalDateTime resolveEndsAt(LocalDateTime startsAt, LocalDateTime endsAt, Boolean unlimitedDuration) {
        if (Boolean.TRUE.equals(unlimitedDuration)) {
            return null;
        }
        if (endsAt == null) {
            return null;
        }
        if (startsAt != null && endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Bitis tarihi baslangictan once olamaz.");
        }
        return endsAt;
    }

    private void validateCouponWindowAndLimit(Coupon coupon) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new AppException(ErrorCode.COUPON_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            throw new AppException(ErrorCode.COUPON_NOT_STARTED);
        }
        if (coupon.getEndsAt() != null && now.isAfter(coupon.getEndsAt())) {
            throw new AppException(ErrorCode.COUPON_EXPIRED);
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new AppException(ErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }
    }

    private void validateUserCouponWindowAndLimit(UserCoupon userCoupon) {
        if (!Boolean.TRUE.equals(userCoupon.getActive())) {
            throw new AppException(ErrorCode.COUPON_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getStartsAt() != null && now.isBefore(userCoupon.getStartsAt())) {
            throw new AppException(ErrorCode.COUPON_NOT_STARTED);
        }
        if (userCoupon.getEndsAt() != null && now.isAfter(userCoupon.getEndsAt())) {
            throw new AppException(ErrorCode.COUPON_EXPIRED);
        }

        if (userCoupon.getUsageLimit() != null && userCoupon.getUsedCount() != null && userCoupon.getUsedCount() >= userCoupon.getUsageLimit()) {
            throw new AppException(ErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }
    }

    private CouponManageItemResponse toManageResponse(Coupon coupon) {
        return new CouponManageItemResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getActive(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getStartsAt(),
                coupon.getEndsAt(),
                coupon.getEndsAt() == null,
                userCouponRepository.countByCouponId(coupon.getId())
        );
    }

    private MyCouponItemResponse toMyCouponResponse(UserCoupon userCoupon) {
        Coupon coupon = userCoupon.getCoupon();
        Integer remainingUsage = null;
        if (userCoupon.getUsageLimit() != null) {
            int used = userCoupon.getUsedCount() == null ? 0 : userCoupon.getUsedCount();
            remainingUsage = Math.max(userCoupon.getUsageLimit() - used, 0);
        }

        return new MyCouponItemResponse(
                userCoupon.getId(),
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                userCoupon.getActive(),
                userCoupon.getUsageLimit(),
                userCoupon.getUsedCount(),
                remainingUsage,
                userCoupon.getStartsAt(),
                userCoupon.getEndsAt(),
                userCoupon.getEndsAt() == null
        );
    }

    public record ResolvedCoupon(Coupon coupon, UserCoupon userCoupon) {
    }
}