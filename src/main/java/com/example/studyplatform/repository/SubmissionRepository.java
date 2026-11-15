package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Submission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    Optional<Submission> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findByAssignmentIdOrderBySubmittedAt(Long assignmentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    @Query("select s from Submission s join fetch s.student join fetch s.assignment where s.id = :id")
    Optional<Submission> findWithDetailsById(@Param("id") Long id);

    @Query("select avg(s.score) from Submission s "
            + "where s.student.id = :studentId and s.assignment.lesson.module.course.id = :courseId "
            + "and s.score is not null")
    Double averageScore(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
