package com.example.studyplatform.repository;

import com.example.studyplatform.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    List<AnswerOption> findByQuestionIdOrderById(Long questionId);
}
