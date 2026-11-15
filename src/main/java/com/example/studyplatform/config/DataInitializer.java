package com.example.studyplatform.config;

import com.example.studyplatform.dto.AssignmentRequest;
import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.GradeRequest;
import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.dto.QuizRequest;
import com.example.studyplatform.dto.QuizRequest.OptionData;
import com.example.studyplatform.dto.QuizRequest.QuestionData;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.ReviewRequest;
import com.example.studyplatform.dto.SubmissionRequest;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.dto.TakeQuizRequest;
import com.example.studyplatform.dto.UserRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.QuestionType;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.repository.UserRepository;
import com.example.studyplatform.service.AssignmentService;
import com.example.studyplatform.service.CategoryService;
import com.example.studyplatform.service.CourseService;
import com.example.studyplatform.service.EnrollmentService;
import com.example.studyplatform.service.QuizService;
import com.example.studyplatform.service.ReviewService;
import com.example.studyplatform.service.SubmissionService;
import com.example.studyplatform.service.TagService;
import com.example.studyplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Демо-данные для запуска «из коробки» (включается {@code app.seed-demo-data=true}, по умолчанию в профиле dev).
 * Если есть хотя бы один пользователь, всё наполнение пропускается. Данные создаются через сервисный слой,
 * поэтому заодно демонстрируют бизнес-сценарии.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService users;
    private final CategoryService categories;
    private final TagService tags;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final AssignmentService assignments;
    private final SubmissionService submissions;
    private final QuizService quizzes;
    private final ReviewService reviews;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("База не пуста - демо-данные пропущены");
            return;
        }
        UserResponse teacher = users.create(new UserRequest("Анна Иванова", "teacher@study.local", Role.TEACHER,
                new UserRequest.ProfileData("Senior Java-разработчик, 8 лет с Hibernate", null, "+7-900-000-00-01")));
        UserResponse alice = users.create(new UserRequest("Алиса Петрова", "alice@study.local", Role.STUDENT,
                new UserRequest.ProfileData("Изучаю Java", null, null)));
        UserResponse bob = users.create(new UserRequest("Борис Сидоров", "bob@study.local", Role.STUDENT, null));
        users.create(new UserRequest("Админ", "admin@study.local", Role.ADMIN, null));

        Long programming = categories.create(new NameRequest("Программирование")).id();
        Long databases = categories.create(new NameRequest("Базы данных")).id();
        Set<Long> tagIds = Set.of(tags.create(new NameRequest("Java")).id(),
                tags.create(new NameRequest("Hibernate")).id(),
                tags.create(new NameRequest("Beginner")).id());

        CourseDetails hibernate = courses.create(new CourseRequest("Основы Hibernate",
                "Введение в ORM: сущности, связи, ленивая загрузка и транзакции", 40, LocalDate.now(),
                programming, teacher.id(), tagIds, List.of(
                new ModuleRequest("Введение в ORM", 1, "Зачем нужен ORM", List.of(
                        new LessonRequest("Что такое ORM", "Object-Relational Mapping...", null, 1),
                        new LessonRequest("Первая сущность", "@Entity, @Id, @GeneratedValue", null, 2))),
                new ModuleRequest("Связи и загрузка", 2, "OneToMany, ManyToMany, Lazy", List.of(
                        new LessonRequest("Типы связей", "1-1, 1-M, M-M", null, 1),
                        new LessonRequest("Lazy и N+1", "LazyInitializationException, JOIN FETCH", null, 2))))));
        courses.create(new CourseRequest("PostgreSQL для разработчика", "Индексы, транзакции, планы запросов",
                24, LocalDate.now().plusDays(14), databases, teacher.id(), Set.of(), null));

        Long course = hibernate.course().id();
        Long firstLesson = hibernate.modules().get(0).lessons().get(0).id();
        Long secondModule = hibernate.modules().get(1).id();

        AssignmentResponse assignment = assignments.create(firstLesson, new AssignmentRequest("Мини-проект: сущность User",
                "Опишите сущность User и сохраните её через репозиторий", LocalDateTime.now().plusDays(30), 100));

        enrollments.enrollStudent(course, alice.id());
        enrollments.enrollStudent(course, bob.id());

        SubmissionResponse submission = submissions.submit(assignment.id(),
                new SubmissionRequest(alice.id(), "https://github.com/alice/user-entity"));
        submissions.grade(submission.id(), new GradeRequest(95, "Отличная работа!"));

        QuizResponse quiz = quizzes.create(secondModule, new QuizRequest("Тест по связям и загрузке", 15, 60, List.of(
                new QuestionData("Какая стратегия загрузки у @OneToMany по умолчанию?", QuestionType.SINGLE_CHOICE, List.of(
                        new OptionData("LAZY", true), new OptionData("EAGER", false))),
                new QuestionData("Как избежать LazyInitializationException?", QuestionType.MULTIPLE_CHOICE, List.of(
                        new OptionData("JOIN FETCH", true), new OptionData("@EntityGraph", true),
                        new OptionData("Игнорировать исключение", false))))));
        Map<Long, Set<Long>> answers = Map.of(
                quiz.questions().get(0).id(), Set.of(quiz.questions().get(0).options().get(0).id()),
                quiz.questions().get(1).id(), Set.of(quiz.questions().get(1).options().get(0).id(),
                        quiz.questions().get(1).options().get(1).id()));
        quizzes.takeQuiz(quiz.id(), new TakeQuizRequest(alice.id(), answers));

        reviews.add(course, new ReviewRequest(alice.id(), 5, "Очень понятно объясняют ленивую загрузку"));
        log.info("Демо-данные созданы: преподаватель {}, студенты {} и {}", teacher.email(), alice.email(), bob.email());
    }
}
