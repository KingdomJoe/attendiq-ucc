package com.ucc.attendance.dto;

import com.ucc.attendance.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String identifier,
            @NotBlank String password,
            UserRole role
    ) {}

    public record StudentRegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String indexNumber,
            @NotBlank String departmentCode,
            @NotBlank @Size(min = 8) String password
    ) {}

    public record LecturerRegisterRequest(
            @NotBlank String name,
            @NotBlank String lecturerCode,
            @NotBlank String departmentCode,
            @NotBlank @Size(min = 8) String password
    ) {}

    public record AuthResponse(
            String token,
            UserRole role,
            Long userId,
            String displayName
    ) {}

    public record MeResponse(
            Long userId,
            UserRole role,
            String displayName,
            String emailOrCode,
            String indexNumber
    ) {}

    public record UpdateProfileRequest(
            @NotBlank String name,
            String currentPassword,
            @Size(min = 8) String newPassword
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank String identifier,
            @NotNull UserRole role,
            @NotBlank @Size(min = 8) String newPassword,
            @NotBlank String confirmPassword
    ) {}
}
