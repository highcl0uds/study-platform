package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseIdOrderByOrderIndex(Long courseId);

    /**
     * Модули курса вместе с уроками и тестом одним запросом. Тест подтягивается явно: инверсная сторона
     * optional-связи 1-1 (mappedBy) без bytecode enhancement может требовать дополнительных запросов (N+1).
     */
    @Query("select distinct m from Module m left join fetch m.lessons left join fetch m.quiz "
            + "where m.course.id = :courseId order by m.orderIndex")
    List<Module> findWithLessonsByCourseId(@Param("courseId") Long courseId);

    boolean existsByCourseIdAndOrderIndex(Long courseId, Integer orderIndex);

    boolean existsByCourseIdAndOrderIndexAndIdNot(Long courseId, Integer orderIndex, Long id);
}
