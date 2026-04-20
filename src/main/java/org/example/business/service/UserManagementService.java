package org.example.business.service;

import lombok.RequiredArgsConstructor;
import org.example.common.dto.auth.MessageResponse;
import org.example.common.dto.user.UserListItemResponse;
import org.example.common.dto.user.UserUpdateRequest;
import org.example.common.entity.AppRole;
import org.example.common.entity.AppUser;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.data.repository.AppRoleRepository;
import org.example.data.repository.AppUserRepository;
import org.example.data.repository.AuthTokenRepository;
import org.example.data.repository.CartItemRepository;
import org.example.data.repository.CartRepository;
import org.example.data.repository.OrderRepository;
import org.example.data.repository.ProductFavoriteRepository;
import org.example.data.repository.ProductReviewRepository;
import org.example.data.repository.UserCouponRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductFavoriteRepository productFavoriteRepository;
    private final ProductReviewRepository productReviewRepository;
    private final UserCouponRepository userCouponRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserListItemResponse> listUsers() {
        return appUserRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserListItemResponse updateUser(Integer userId, UserUpdateRequest request, String actorUsername, String actorFullName, String actorRole) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (hasText(request.getEmail())) {
            String email = request.getEmail().trim();
            boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
            if (emailChanged && appUserRepository.existsByEmail(email)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(email);
        }

        if (hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (hasText(request.getAddress())) {
            user.setAddress(request.getAddress().trim());
        }

        if (hasText(request.getRole())) {
            String normalizedRole = request.getRole().trim().toUpperCase();
            AppRole role = appRoleRepository.findByName(normalizedRole)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_ROLE_NOT_FOUND));
            user.setRole(role);
        }

        AppUser updated = appUserRepository.save(user);

        auditLogService.logAs(
                actorUsername,
                actorFullName,
                actorRole,
                "UPDATE",
                "USERS",
                updated.getId(),
                "User updated: " + updated.getUsername()
        );

        return toResponse(updated);
    }

    @Transactional
    public MessageResponse deleteUser(Integer userId, String actorUsername, String actorFullName, String actorRole) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (orderRepository.existsByUserId(userId)) {
            throw new AppException(ErrorCode.USER_HAS_ORDERS);
        }

        String deletedUsername = user.getUsername();

        authTokenRepository.deleteByUserId(userId);
        cartItemRepository.deleteByUserId(userId);
        productFavoriteRepository.deleteByUserId(userId);
        productReviewRepository.deleteByUserId(userId);
        userCouponRepository.deleteByUserId(userId);
        cartRepository.deleteByUserId(userId);
        appUserRepository.delete(user);

        auditLogService.logAs(
                actorUsername,
                actorFullName,
                actorRole,
                "DELETE",
                "USERS",
                userId,
                "User deleted: " + deletedUsername
        );

        return new MessageResponse("Kullanici silindi.");
    }

    private UserListItemResponse toResponse(AppUser user) {
        List<String> permissions = user.getRole().getPermissions().stream()
                .map(permission -> permission.getCode())
                .sorted(Comparator.naturalOrder())
                .toList();

        return new UserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getRole().getName().toLowerCase(),
                permissions
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}