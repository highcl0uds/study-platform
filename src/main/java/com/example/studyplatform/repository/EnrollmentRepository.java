package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Enrollment;
import com.example.studyplatform.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByStudentIdOrderByEnrollDateDesc(Long studentId);

    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByCourseIdOrderByEnrollDateAsc(Long courseId);

    long countByCourseId(Long courseId);
}
