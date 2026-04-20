package org.example.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new AppException(ErrorCode.USER_RESOLUTION_FAILED);
    }

    public String getCurrentUsernameOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return "system";
    }

    public String getCurrentUserFullNameOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails userDetails) {
            return userDetails.getFullName();
        }
        return "System";
    }

    public String getCurrentRoleOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails userDetails) {
            return userDetails.getRoleName();
        }
        return "SYSTEM";
    }
}
