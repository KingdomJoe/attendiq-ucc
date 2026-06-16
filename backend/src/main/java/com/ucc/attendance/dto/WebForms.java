package com.ucc.attendance.dto;

import com.ucc.attendance.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class WebForms {

    private WebForms() {}

    public static class LoginForm {
        @NotBlank(message = "Email or lecturer code is required")
        private String identifier;

        @NotBlank(message = "Password is required")
        private String password;

        @NotNull(message = "Please select a role")
        private UserRole role;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
    }

    public static class StudentRegisterForm {
        @NotBlank(message = "Full name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        private String email;

        @NotBlank(message = "Index number is required")
        private String indexNumber;

        @NotBlank(message = "Department is required")
        private String departmentCode;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getIndexNumber() { return indexNumber; }
        public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LecturerRegisterForm {
        @NotBlank(message = "Full name is required")
        private String name;

        @NotBlank(message = "Lecturer code is required")
        private String lecturerCode;

        @NotBlank(message = "Department is required")
        private String departmentCode;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLecturerCode() { return lecturerCode; }
        public void setLecturerCode(String lecturerCode) { this.lecturerCode = lecturerCode; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ProfileForm {
        @NotBlank(message = "Display name is required")
        private String name;

        private String currentPassword;

        private String newPassword;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ForgotPasswordForm {
        @NotNull(message = "Please select a role")
        private UserRole role;

        @NotBlank(message = "Index number or lecturer code is required")
        private String identifier;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;

        @NotBlank(message = "Please confirm your password")
        private String confirmPassword;

        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
