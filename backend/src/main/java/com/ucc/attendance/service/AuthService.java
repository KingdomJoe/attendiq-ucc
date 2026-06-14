package com.ucc.attendance.service;

import com.ucc.attendance.config.AppProperties;
import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.AuthDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.JwtService;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final LecturerCodeRepository lecturerCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Transactional
    public AuthDtos.AuthResponse registerStudent(AuthDtos.StudentRegisterRequest req) {
        if (!req.email().matches(appProperties.studentEmailPattern())) {
            throw new ApiException("INVALID_EMAIL", "Email must be an institutional address", HttpStatus.BAD_REQUEST);
        }
        if (studentRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException("EMAIL_EXISTS", "Email already registered", HttpStatus.CONFLICT);
        }
        if (studentRepository.existsByIndexNumberIgnoreCase(req.indexNumber())) {
            throw new ApiException("INDEX_EXISTS", "Index number already registered", HttpStatus.CONFLICT);
        }
        Department dept = departmentRepository.findByCodeIgnoreCase(req.departmentCode())
                .orElseThrow(() -> new ApiException("INVALID_DEPARTMENT", "Department not found", HttpStatus.BAD_REQUEST));

        Student student = Student.builder()
                .name(req.name())
                .email(req.email().toLowerCase())
                .indexNumber(req.indexNumber().toUpperCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .department(dept)
                .build();
        courseRepository.findAllOrdered().stream()
                .filter(c -> c.getDepartment().getId().equals(dept.getId()))
                .forEach(student.getCourses()::add);
        studentRepository.save(student);

        String token = jwtService.generateToken(student.getId(), student.getEmail(), UserRole.STUDENT);
        return new AuthDtos.AuthResponse(token, UserRole.STUDENT, student.getId(), student.getName());
    }

    @Transactional
    public AuthDtos.AuthResponse registerStudentForCourse(String enrollmentToken, AuthDtos.StudentRegisterRequest req) {
        Course course = courseRepository.findByEnrollmentToken(enrollmentToken.trim())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Invalid enrollment link", HttpStatus.NOT_FOUND));

        if (!req.email().matches(appProperties.studentEmailPattern())) {
            throw new ApiException("INVALID_EMAIL", "Email must be an institutional address", HttpStatus.BAD_REQUEST);
        }
        if (studentRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException("EMAIL_EXISTS", "Email already registered", HttpStatus.CONFLICT);
        }
        if (studentRepository.existsByIndexNumberIgnoreCase(req.indexNumber())) {
            throw new ApiException("INDEX_EXISTS", "Index number already registered", HttpStatus.CONFLICT);
        }

        Department dept = departmentRepository.findByCodeIgnoreCase(req.departmentCode())
                .orElseThrow(() -> new ApiException("INVALID_DEPARTMENT", "Department not found", HttpStatus.BAD_REQUEST));

        Student student = Student.builder()
                .name(req.name())
                .email(req.email().toLowerCase())
                .indexNumber(req.indexNumber().toUpperCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .department(dept)
                .build();
        student.getCourses().add(course);
        studentRepository.save(student);

        String token = jwtService.generateToken(student.getId(), student.getEmail(), UserRole.STUDENT);
        return new AuthDtos.AuthResponse(token, UserRole.STUDENT, student.getId(), student.getName());
    }

    @Transactional
    public AuthDtos.AuthResponse registerLecturer(AuthDtos.LecturerRegisterRequest req) {
        // Validate the lecturer code against the pre-generated codes table
        LecturerCode lecturerCode = lecturerCodeRepository.findByCodeIgnoreCase(req.lecturerCode().trim())
                .orElseThrow(() -> new ApiException("INVALID_CODE",
                        "Invalid lecturer code. Please use a valid registration code provided by the institution.",
                        HttpStatus.BAD_REQUEST));

        if (lecturerCode.isClaimed()) {
            throw new ApiException("CODE_CLAIMED",
                    "This lecturer code has already been used for registration.",
                    HttpStatus.CONFLICT);
        }

        if (lecturerRepository.existsByLecturerCodeIgnoreCase(req.lecturerCode())) {
            throw new ApiException("CODE_EXISTS", "Lecturer code already registered", HttpStatus.CONFLICT);
        }

        // Auto-assign department from the lecturer code
        Department dept = lecturerCode.getDepartment();

        Lecturer lecturer = Lecturer.builder()
                .name(req.name())
                .lecturerCode(req.lecturerCode().toUpperCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .department(dept)
                .build();
        lecturer = lecturerRepository.save(lecturer);

        // Mark the code as claimed
        lecturerCode.setClaimed(true);
        lecturerCode.setClaimedBy(lecturer);
        lecturerCode.setClaimedAt(Instant.now());
        lecturerCodeRepository.save(lecturerCode);

        String token = jwtService.generateToken(lecturer.getId(), lecturer.getLecturerCode(), UserRole.LECTURER);
        return new AuthDtos.AuthResponse(token, UserRole.LECTURER, lecturer.getId(), lecturer.getName());
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        if (req.role() == UserRole.STUDENT) {
            Student student = studentRepository.findByEmailIgnoreCase(req.identifier())
                    .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED));
            if (!passwordEncoder.matches(req.password(), student.getPasswordHash())) {
                throw new ApiException("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
            }
            String token = jwtService.generateToken(student.getId(), student.getEmail(), UserRole.STUDENT);
            return new AuthDtos.AuthResponse(token, UserRole.STUDENT, student.getId(), student.getName());
        }
        if (req.role() == UserRole.LECTURER) {
            Lecturer lecturer = lecturerRepository.findByLecturerCodeIgnoreCase(req.identifier())
                    .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid code or password", HttpStatus.UNAUTHORIZED));
            if (!passwordEncoder.matches(req.password(), lecturer.getPasswordHash())) {
                throw new ApiException("INVALID_CREDENTIALS", "Invalid code or password", HttpStatus.UNAUTHORIZED);
            }
            String token = jwtService.generateToken(lecturer.getId(), lecturer.getLecturerCode(), UserRole.LECTURER);
            return new AuthDtos.AuthResponse(token, UserRole.LECTURER, lecturer.getId(), lecturer.getName());
        }
        throw new ApiException("INVALID_ROLE", "Role is required", HttpStatus.BAD_REQUEST);
    }

    public AuthDtos.MeResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        if (principal.getRole() == UserRole.STUDENT) {
            Student student = studentRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
            return new AuthDtos.MeResponse(
                    student.getId(),
                    UserRole.STUDENT,
                    student.getName(),
                    student.getEmail(),
                    student.getIndexNumber()
            );
        }
        Lecturer lecturer = lecturerRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        return new AuthDtos.MeResponse(
                lecturer.getId(),
                UserRole.LECTURER,
                lecturer.getName(),
                lecturer.getLecturerCode(),
                null
        );
    }
}
