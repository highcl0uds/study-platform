package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /** Задание вместе с цепочкой урок -> модуль -> курс (один запрос вместо трёх ленивых). */
    @Query("select a from Assignment a join fetch a.lesson l join fetch l.module m join fetch m.course "
            + "where a.id = :id")
    Optional<Assignment> findWithCourseById(@Param("id") Long id);

    List<Assignment> findByLessonIdOrderById(Long lessonId);

    List<Assignment> findByLessonModuleCourseIdOrderById(Long courseId);
}
