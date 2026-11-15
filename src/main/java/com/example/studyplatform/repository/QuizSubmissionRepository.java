package com.example.studyplatform.repository;

import com.example.studyplatform.entity.QuizSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByStudentIdOrderByTakenAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByQuizIdOrderByTakenAtDesc(Long quizId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByQuizModuleCourseIdOrderByTakenAtDesc(Long courseId);
}
