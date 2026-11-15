package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByModuleId(Long moduleId);

    boolean existsByModuleId(Long moduleId);

    List<Quiz> findByModuleCourseIdOrderById(Long courseId);

    @Query("select q from Quiz q join fetch q.module m join fetch m.course where q.id = :id")
    Optional<Quiz> findWithModuleById(@Param("id") Long id);
}
