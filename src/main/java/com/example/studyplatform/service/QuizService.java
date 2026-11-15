package com.example.studyplatform.service;

import com.example.studyplatform.dto.QuizRequest;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.TakeQuizRequest;
import com.example.studyplatform.entity.AnswerOption;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Question;
import com.example.studyplatform.entity.QuestionType;
import com.example.studyplatform.entity.Quiz;
import com.example.studyplatform.entity.QuizAnswer;
import com.example.studyplatform.entity.QuizSubmission;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.LearningMapper;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.QuestionRepository;
import com.example.studyplatform.repository.QuizRepository;
import com.example.studyplatform.repository.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuizService {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final QuizSubmissionRepository results;
    private final EnrollmentRepository enrollments;
    private final EntityLookup lookup;
    private final Clock clock;

    public QuizResponse create(Long moduleId, QuizRequest request) {
        Module module = lookup.module(moduleId);
        if (quizzes.existsByModuleId(moduleId)) {
            throw new ConflictException("У модуля id=" + moduleId + " уже есть тест");
        }
        Quiz quiz = new Quiz();
        quiz.setModule(module);
        quiz.setTitle(request.title());
        quiz.setTimeLimit(request.timeLimit());
        if (request.passingPercent() != null) {
            quiz.setPassingPercent(request.passingPercent());
        }
        request.questions().forEach(q -> quiz.addQuestion(buildQuestion(q)));
        quizzes.save(quiz);
        log.info("Создан тест id={} для модуля id={} (вопросов: {})", quiz.getId(), moduleId, request.questions().size());
        return toResponse(quiz);
    }

    @Transactional(readOnly = true)
    public QuizResponse get(Long id) {
        return toResponse(findWithModule(id));
    }

    @Transactional(readOnly = true)
    public QuizResponse getByModule(Long moduleId) {
        lookup.module(moduleId);
        Quiz quiz = quizzes.findByModuleId(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("У модуля id=" + moduleId + " нет теста"));
        return toResponse(quiz);
    }

    public void delete(Long id) {
        Quiz quiz = findWithModule(id);
        quiz.getModule().setQuiz(null);   // иначе каскад PERSIST от Module.quiz воскресит удаляемый тест
        quizzes.delete(quiz);
        log.info("Удалён тест id={}", id);
    }

    public QuizResultResponse takeQuiz(Long quizId, TakeQuizRequest request) {
        Quiz quiz = findWithModule(quizId);
        User student = lookup.student(request.studentId());
        Long courseId = quiz.getModule().getCourse().getId();
        if (!enrollments.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException("Студент id=" + student.getId() + " не записан на курс id=" + courseId);
        }
        List<Question> quizQuestions = questions.findWithOptionsByQuizId(quizId);
        Map<Long, Question> byId = quizQuestions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        validateAnswers(request.answers(), byId);

        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setTakenAt(LocalDateTime.now(clock));
        int score = 0;
        for (Question question : quizQuestions) {
            Set<Long> selected = selectedFor(request.answers(), question.getId());
            if (selected.equals(correctIds(question))) {
                score++;
            }
            question.getOptions().stream()
                    .filter(o -> selected.contains(o.getId()))
                    .forEach(o -> submission.addAnswer(answer(question, o)));
        }
        int max = quizQuestions.size();
        int percent = max == 0 ? 0 : Math.round(score * 100f / max);
        submission.setScore(score);
        submission.setMaxScore(max);
        submission.setPercent(percent);
        submission.setPassed(percent >= quiz.getPassingPercent());
        results.save(submission);
        log.info("Студент id={} прошёл тест id={}: {}/{} ({}%)", student.getId(), quizId, score, max, percent);
        return LearningMapper.toResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> resultsByStudent(Long studentId) {
        lookup.user(studentId);
        return results.findByStudentIdOrderByTakenAtDesc(studentId).stream().map(LearningMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> resultsByQuiz(Long quizId) {
        findWithModule(quizId);
        return results.findByQuizIdOrderByTakenAtDesc(quizId).stream().map(LearningMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<QuizResultResponse> resultsByCourse(Long courseId) {
        lookup.course(courseId);
        return results.findByQuizModuleCourseIdOrderByTakenAtDesc(courseId).stream()
                .map(LearningMapper::toResponse).toList();
    }

    private Quiz findWithModule(Long id) {
        return quizzes.findWithModuleById(id).orElseThrow(() -> new ResourceNotFoundException("Тест", id));
    }

    private QuizResponse toResponse(Quiz quiz) {
        return LearningMapper.toResponse(quiz, questions.findWithOptionsByQuizId(quiz.getId()));
    }

    private Question buildQuestion(QuizRequest.QuestionData data) {
        QuestionType type = data.type() != null ? data.type() : QuestionType.SINGLE_CHOICE;
        long correct = data.options().stream().filter(QuizRequest.OptionData::correct).count();
        if (data.options().size() < 2) {
            throw new BusinessRuleException("У вопроса '" + data.text() + "' должно быть минимум 2 варианта");
        }
        if (correct == 0 || (type == QuestionType.SINGLE_CHOICE && correct != 1)) {
            throw new BusinessRuleException("Неверное число правильных ответов у вопроса '" + data.text() + "'");
        }
        Question question = new Question();
        question.setText(data.text());
        question.setType(type);
        data.options().forEach(o -> {
            AnswerOption option = new AnswerOption();
            option.setText(o.text());
            option.setCorrect(o.correct());
            question.addOption(option);
        });
        return question;
    }

    private void validateAnswers(Map<Long, Set<Long>> answers, Map<Long, Question> questionsById) {
        answers.forEach((questionId, nullableIds) -> {
            Set<Long> optionIds = nullableIds == null ? Collections.emptySet() : nullableIds;
            Question question = questionsById.get(questionId);
            if (question == null) {
                throw new BusinessRuleException("Вопрос id=" + questionId + " не относится к этому тесту");
            }
            Set<Long> validIds = question.getOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet());
            if (!validIds.containsAll(optionIds)) {
                throw new BusinessRuleException("Неверные варианты ответа для вопроса id=" + questionId);
            }
            if (question.getType() == QuestionType.SINGLE_CHOICE && optionIds.size() > 1) {
                throw new BusinessRuleException("Вопрос id=" + questionId + " допускает только один вариант");
            }
        });
    }

    private Set<Long> selectedFor(Map<Long, Set<Long>> answers, Long questionId) {
        Set<Long> selected = answers.get(questionId);
        return selected == null ? Collections.emptySet() : selected;
    }

    private Set<Long> correctIds(Question question) {
        return question.getOptions().stream().filter(AnswerOption::isCorrect)
                .map(AnswerOption::getId).collect(Collectors.toSet());
    }

    private QuizAnswer answer(Question question, AnswerOption option) {
        QuizAnswer answer = new QuizAnswer();
        answer.setQuestion(question);
        answer.setSelectedOption(option);
        return answer;
    }
}
