package com.ucc.attendance.config;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (lecturerRepository.count() > 0) {
            return;
        }

        Department csc = departmentRepository.findByCodeIgnoreCase("CSC")
                .orElseGet(() -> departmentRepository.save(Department.builder().code("CSC").name("Computer Science").build()));

        Course course = courseRepository.findByCourseCodeIgnoreCase("CSC301")
                .orElseGet(() -> courseRepository.save(Course.builder()
                        .courseCode("CSC301")
                        .courseName("Java Programming")
                        .department(csc)
                        .build()));

        Lecturer lecturer = Lecturer.builder()
                .name("Demo Lecturer")
                .lecturerCode("LEC001")
                .passwordHash(passwordEncoder.encode("lecturer123"))
                .department(csc)
                .build();
        lecturer.getCourses().add(course);
        lecturerRepository.save(lecturer);

        Student student = Student.builder()
                .name("Demo Student")
                .email("student@ucc.edu.gh")
                .indexNumber("STU2026001")
                .passwordHash(passwordEncoder.encode("student123"))
                .department(csc)
                .build();
        student.getCourses().add(course);
        studentRepository.save(student);
    }
}
