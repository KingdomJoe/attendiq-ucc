package com.ucc.attendance.controller;

import com.ucc.attendance.dto.AuthDtos;
import com.ucc.attendance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/student/register")
    public AuthDtos.AuthResponse registerStudent(@Valid @RequestBody AuthDtos.StudentRegisterRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/lecturer/register")
    public AuthDtos.AuthResponse registerLecturer(@Valid @RequestBody AuthDtos.LecturerRegisterRequest request) {
        return authService.registerLecturer(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.MeResponse me() {
        return authService.me();
    }

    @PatchMapping("/me")
    public AuthDtos.MeResponse updateProfile(@Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }
}
