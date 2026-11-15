package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = {"category", "teacher"})
    @Query("select c from Course c order by c.title")
    List<Course> findAllWithDetails();

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByCategoryIdOrderByTitle(Long categoryId);

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByTeacherIdOrderByTitle(Long teacherId);

    @EntityGraph(attributePaths = {"category", "teacher"})
    List<Course> findByTitleContainingIgnoreCaseOrderByTitle(String title);

    @EntityGraph(attributePaths = {"category", "teacher"})
    @Query("select c from Course c join c.tags t where t.name = :tagName order by c.title")
    List<Course> findByTagName(@Param("tagName") String tagName);

    /** Курс с модулями (JOIN FETCH). Уроки подгружаются отдельным запросом ModuleRepository. */
    @Query("select distinct c from Course c "
            + "join fetch c.category join fetch c.teacher "
            + "left join fetch c.modules m left join fetch m.quiz where c.id = :id")
    Optional<Course> findWithModulesById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"tags"})
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findWithTagsById(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByTeacherId(Long teacherId);
}
