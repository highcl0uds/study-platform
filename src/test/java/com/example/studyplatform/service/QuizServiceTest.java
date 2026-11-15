package com.example.studyplatform.service;

import com.example.studyplatform.dto.QuizRequest;
import com.example.studyplatform.dto.QuizRequest.OptionData;
import com.example.studyplatform.dto.QuizRequest.QuestionData;
import com.example.studyplatform.dto.TakeQuizRequest;
import com.example.studyplatform.entity.AnswerOption;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Question;
import com.example.studyplatform.entity.QuestionType;
import com.example.studyplatform.entity.Quiz;
import com.example.studyplatform.entity.QuizSubmission;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.QuestionRepository;
import com.example.studyplatform.repository.QuizRepository;
import com.example.studyplatform.repository.QuizSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock private QuizRepository quizzes;
    @Mock private QuestionRepository questions;
    @Mock private QuizSubmissionRepository results;
    @Mock private EnrollmentRepository enrollments;
    @Mock private EntityLookup lookup;

    private QuizService service;
    private Quiz quiz;
    private Question single;
    private Question multiple;
    private User student;

    @BeforeEach
    void setUp() {
        service = new QuizService(quizzes, questions, results, enrollments, lookup,
                Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC));

        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", 1L);
        Module module = new Module();
        ReflectionTestUtils.setField(module, "id", 2L);
        module.setCourse(course);
        quiz = new Quiz();
        ReflectionTestUtils.setField(quiz, "id", 3L);
        quiz.setModule(module);
        quiz.setTitle("Quiz");
        quiz.setPassingPercent(60);

        single = question(10L, QuestionType.SINGLE_CHOICE, option(100L, true), option(101L, false));
        multiple = question(20L, QuestionType.MULTIPLE_CHOICE, option(200L, true), option(201L, true), option(202L, false));

        student = new User();
        ReflectionTestUtils.setField(student, "id", 4L);
        student.setRole(Role.STUDENT);
        student.setName("Carol");
    }

    private void givenQuizAndEnrolledStudent() {
        when(quizzes.findWithModuleById(3L)).thenReturn(Optional.of(quiz));
        when(lookup.student(4L)).thenReturn(student);
        when(enrollments.existsByStudentIdAndCourseId(4L, 1L)).thenReturn(true);
        when(questions.findWithOptionsByQuizId(3L)).thenReturn(List.of(single, multiple));
    }

    @Test
    void allCorrectGivesFullScore() {
        givenQuizAndEnrolledStudent();

        var result = service.takeQuiz(3L, new TakeQuizRequest(4L,
                Map.of(10L, Set.of(100L), 20L, Set.of(200L, 201L))));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void unansweredQuestionsCountAsWrong() {
        givenQuizAndEnrolledStudent();

        var result = service.takeQuiz(3L, new TakeQuizRequest(4L, Map.of(10L, Set.of(100L))));

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.percent()).isEqualTo(50);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void storesSelectedOptionsWithTheSubmission() {
        givenQuizAndEnrolledStudent();

        service.takeQuiz(3L, new TakeQuizRequest(4L, Map.of(10L, Set.of(101L), 20L, Set.of(202L))));

        ArgumentCaptor<QuizSubmission> captor = ArgumentCaptor.forClass(QuizSubmission.class);
        verify(results).save(captor.capture());
        assertThat(captor.getValue().getAnswers()).hasSize(2);
        assertThat(captor.getValue().getScore()).isZero();
    }

    @Test
    void severalOptionsForSingleChoiceAreRejected() {
        givenQuizAndEnrolledStudent();

        assertThatThrownBy(() -> service.takeQuiz(3L, new TakeQuizRequest(4L, Map.of(10L, Set.of(100L, 101L)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void notEnrolledStudentCannotTakeQuiz() {
        when(quizzes.findWithModuleById(3L)).thenReturn(Optional.of(quiz));
        when(lookup.student(4L)).thenReturn(student);
        when(enrollments.existsByStudentIdAndCourseId(4L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.takeQuiz(3L, new TakeQuizRequest(4L, Map.of())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void quizDefinitionNeedsExactlyOneCorrectOptionForSingleChoice() {
        when(lookup.module(2L)).thenReturn(quiz.getModule());
        var bad = new QuizRequest("Q", null, null, List.of(new QuestionData("text", QuestionType.SINGLE_CHOICE,
                List.of(new OptionData("a", true), new OptionData("b", true)))));

        assertThatThrownBy(() -> service.create(2L, bad)).isInstanceOf(BusinessRuleException.class);
    }

    private Question question(Long id, QuestionType type, AnswerOption... options) {
        Question question = new Question();
        ReflectionTestUtils.setField(question, "id", id);
        question.setText("q" + id);
        question.setType(type);
        for (AnswerOption option : options) {
            question.addOption(option);
        }
        return question;
    }

    private AnswerOption option(Long id, boolean correct) {
        AnswerOption option = new AnswerOption();
        ReflectionTestUtils.setField(option, "id", id);
        option.setText("o" + id);
        option.setCorrect(correct);
        return option;
    }
}
