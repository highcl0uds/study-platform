package com.example.studyplatform.integration;

import com.example.studyplatform.dto.AssignmentRequest;
import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.dto.QuizRequest;
import com.example.studyplatform.dto.QuizRequest.OptionData;
import com.example.studyplatform.dto.QuizRequest.QuestionData;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.UserRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.QuestionType;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.service.AssignmentService;
import com.example.studyplatform.service.CategoryService;
import com.example.studyplatform.service.CourseContentService;
import com.example.studyplatform.service.CourseService;
import com.example.studyplatform.service.EnrollmentService;
import com.example.studyplatform.service.ProgressService;
import com.example.studyplatform.service.QuizService;
import com.example.studyplatform.service.ReviewService;
import com.example.studyplatform.service.SubmissionService;
import com.example.studyplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected UserService userService;
    @Autowired protected CategoryService categoryService;
    @Autowired protected CourseService courseService;
    @Autowired protected CourseContentService contentService;
    @Autowired protected AssignmentService assignmentService;
    @Autowired protected EnrollmentService enrollmentService;
    @Autowired protected SubmissionService submissionService;
    @Autowired protected QuizService quizService;
    @Autowired protected ReviewService reviewService;
    @Autowired protected ProgressService progressService;

    protected static int next() {
        return SEQ.incrementAndGet();
    }

    protected UserResponse newUser(Role role) {
        int n = next();
        return userService.create(new UserRequest(role + " " + n, role.name().toLowerCase() + n + "@test.io", role, null));
    }

    protected Long newCategory() {
        return categoryService.create(new NameRequest("Category " + next())).id();
    }

    protected CourseDetails newCourse(Long teacherId) {
        return courseService.create(new CourseRequest("Course " + next(), "Описание", 10, null,
                newCategory(), teacherId, null, List.of(
                new ModuleRequest("M1", 1, null, List.of(new LessonRequest("L1", "text", null, 1),
                        new LessonRequest("L2", "text", null, 2))),
                new ModuleRequest("M2", 2, null, List.of(new LessonRequest("L3", "text", null, 1))))));
    }

    protected Long firstLessonId(CourseDetails course) {
        return course.modules().get(0).lessons().get(0).id();
    }

    protected Long newAssignment(Long lessonId) {
        return assignmentService.create(lessonId,
                new AssignmentRequest("HW", "do it", LocalDateTime.now().plusDays(7), 100)).id();
    }

    protected QuizResponse newQuiz(Long moduleId) {
        return quizService.create(moduleId, new QuizRequest("Quiz", 10, 50, List.of(
                new QuestionData("Q1", QuestionType.SINGLE_CHOICE,
                        List.of(new OptionData("right", true), new OptionData("wrong", false))),
                new QuestionData("Q2", QuestionType.MULTIPLE_CHOICE,
                        List.of(new OptionData("a", true), new OptionData("b", true), new OptionData("c", false))))));
    }

    protected Map<Long, Set<Long>> perfectAnswers(QuizResponse quiz) {
        Map<Long, Set<Long>> answers = new HashMap<>();
        answers.put(quiz.questions().get(0).id(), Set.of(quiz.questions().get(0).options().get(0).id()));
        answers.put(quiz.questions().get(1).id(), Set.of(quiz.questions().get(1).options().get(0).id(),
                quiz.questions().get(1).options().get(1).id()));
        return answers;
    }

    protected Map<Long, Set<Long>> wrongAnswers(QuizResponse quiz) {
        Map<Long, Set<Long>> answers = new HashMap<>();
        answers.put(quiz.questions().get(0).id(), Set.of(quiz.questions().get(0).options().get(1).id()));
        answers.put(quiz.questions().get(1).id(), Set.of(quiz.questions().get(1).options().get(2).id()));
        return answers;
    }
}
