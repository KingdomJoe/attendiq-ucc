package com.ucc.attendance.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecturer_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LecturerCourse {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private LecturerCourseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lecturerId")
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    @PrePersist
    void prePersist() {
        if (id == null && lecturer != null && course != null) {
            id = new LecturerCourseId(lecturer.getId(), course.getId());
        }
    }
}
