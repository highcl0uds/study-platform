package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("select l from Lesson l join fetch l.module m join fetch m.course where l.id = :id")
    Optional<Lesson> findWithCourseById(@Param("id") Long id);

    List<Lesson> findByModuleIdOrderByOrderIndex(Long moduleId);

    long countByModuleCourseId(Long courseId);
}
