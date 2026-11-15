package com.example.studyplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
public class Quiz extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, unique = true)
    private Module module;

    @Column(nullable = false, length = 200)
    private String title;

    private Integer timeLimit;

    @Column(nullable = false)
    private Integer passingPercent = 60;

    /**
     * Имя "attempts" сохраняет нужный порядок каскадов в текущей версии Hibernate:
     * попытки (и выбранные ответы) удаляются до вопросов и вариантов, на которые они ссылаются.
     */
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<QuizSubmission> attempts = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
    }
}
