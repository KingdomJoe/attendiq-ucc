package com.ucc.attendance.util;

import com.ucc.attendance.domain.UserRole;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException("UNAUTHORIZED", "Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }

    public static UserPrincipal requireRole(UserRole role) {
        UserPrincipal principal = currentUser();
        if (principal.getRole() != role) {
            throw new ApiException("FORBIDDEN", "Insufficient permissions", HttpStatus.FORBIDDEN);
        }
        return principal;
    }
}
