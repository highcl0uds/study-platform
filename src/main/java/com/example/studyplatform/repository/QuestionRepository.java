package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderById(Long quizId);

    @Query("select distinct q from Question q left join fetch q.options where q.quiz.id = :quizId order by q.id")
    List<Question> findWithOptionsByQuizId(@Param("quizId") Long quizId);
}
