package com.ucc.attendance.security;

import com.ucc.attendance.domain.Lecturer;
import com.ucc.attendance.domain.Student;
import com.ucc.attendance.domain.UserRole;
import com.ucc.attendance.repository.LecturerRepository;
import com.ucc.attendance.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return studentRepository.findByEmailIgnoreCase(username)
                .map(this::toPrincipal)
                .orElseGet(() -> lecturerRepository.findByLecturerCodeIgnoreCase(username)
                        .map(this::toPrincipal)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }

    public UserDetails loadStudentByEmail(String email) {
        Student student = studentRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Student not found"));
        return toPrincipal(student);
    }

    public UserDetails loadLecturerByCode(String code) {
        Lecturer lecturer = lecturerRepository.findByLecturerCodeIgnoreCase(code)
                .orElseThrow(() -> new UsernameNotFoundException("Lecturer not found"));
        return toPrincipal(lecturer);
    }

    private UserPrincipal toPrincipal(Student student) {
        return new UserPrincipal(
                student.getId(),
                student.getEmail(),
                student.getPasswordHash(),
                UserRole.STUDENT,
                student.getName()
        );
    }

    private UserPrincipal toPrincipal(Lecturer lecturer) {
        return new UserPrincipal(
                lecturer.getId(),
                lecturer.getLecturerCode(),
                lecturer.getPasswordHash(),
                UserRole.LECTURER,
                lecturer.getName()
        );
    }
}
