package com.ucc.attendance.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "enrollment_token", nullable = false, unique = true, length = 64)
    private String enrollmentToken;

    @PrePersist
    void prePersist() {
        if (enrollmentToken == null) {
            enrollmentToken = java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }
}
