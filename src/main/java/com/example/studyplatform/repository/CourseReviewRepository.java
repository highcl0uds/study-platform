package com.example.studyplatform.repository;

import com.example.studyplatform.entity.CourseReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = {"student", "course"})
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    @Query("select avg(r.rating) from CourseReview r where r.course.id = :courseId")
    Double averageRating(@Param("courseId") Long courseId);
}
