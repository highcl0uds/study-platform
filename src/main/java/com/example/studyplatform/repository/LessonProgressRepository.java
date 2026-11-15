package com.example.studyplatform.repository;

import com.example.studyplatform.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByStudentIdAndLessonId(Long studentId, Long lessonId);

    long countByStudentIdAndLessonModuleCourseId(Long studentId, Long courseId);
}
