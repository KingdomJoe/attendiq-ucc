package com.ucc.attendance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "lecturers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "lecturer_code", nullable = false, unique = true, length = 50)
    private String lecturerCode;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToMany(mappedBy = "lecturer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<LecturerCourse> courseLinks = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Set<Course> getCourses() {
        return courseLinks.stream()
                .map(LecturerCourse::getCourse)
                .collect(Collectors.toSet());
    }

    public boolean hasCourse(Course course) {
        return courseLinks.stream()
                .anyMatch(link -> link.getCourse().getId().equals(course.getId()));
    }

    public void linkCourse(Course course, boolean active) {
        if (hasCourse(course)) {
            return;
        }
        LecturerCourseId pk = new LecturerCourseId(this.id, course.getId());
        LecturerCourse link = LecturerCourse.builder()
                .id(pk)
                .lecturer(this)
                .course(course)
                .active(active)
                .build();
        courseLinks.add(link);
    }
}
