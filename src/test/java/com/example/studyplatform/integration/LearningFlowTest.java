package com.example.studyplatform.integration;

import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.GradeRequest;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.ReviewRequest;
import com.example.studyplatform.dto.SubmissionRequest;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.dto.TakeQuizRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.SubmissionStatus;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.repository.QuizSubmissionRepository;
import com.example.studyplatform.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningFlowTest extends IntegrationTestBase {

    @Autowired private SubmissionRepository submissions;
    @Autowired private QuizSubmissionRepository quizResults;

    private UserResponse teacher;
    private UserResponse student;
    private CourseDetails course;

    private void givenEnrolledStudent() {
        teacher = newUser(Role.TEACHER);
        student = newUser(Role.STUDENT);
        course = newCourse(teacher.id());
        enrollmentService.enrollStudent(course.course().id(), student.id());
    }

    @Test
    void enrollmentIsUniquePerStudentAndCourse() {
        givenEnrolledStudent();

        assertThatThrownBy(() -> enrollmentService.enrollStudent(course.course().id(), student.id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void onlyStudentsCanEnroll() {
        givenEnrolledStudent();

        assertThatThrownBy(() -> enrollmentService.enrollStudent(course.course().id(), teacher.id()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void enrollmentOfUnknownStudentIsNotFound() {
        givenEnrolledStudent();

        assertThatThrownBy(() -> enrollmentService.enrollStudent(course.course().id(), -1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsStudentsOfCourseAndCoursesOfStudent() {
        givenEnrolledStudent();

        assertThat(enrollmentService.listByCourse(course.course().id()))
                .extracting("studentId").containsExactly(student.id());
        assertThat(enrollmentService.listByStudent(student.id()))
                .extracting("courseId").containsExactly(course.course().id());
    }

    @Test
    void studentCanUnenrollAndThenEnrollAgain() {
        givenEnrolledStudent();

        enrollmentService.unenrollStudent(course.course().id(), student.id());
        assertThat(enrollmentService.listByStudent(student.id())).isEmpty();
        assertThatThrownBy(() -> enrollmentService.unenrollStudent(course.course().id(), student.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        enrollmentService.enrollStudent(course.course().id(), student.id());
        assertThat(enrollmentService.listByStudent(student.id())).hasSize(1);
    }

    @Test
    void submissionIsAcceptedOnceAndCanBeGraded() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));

        SubmissionResponse submitted = submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "solution"));
        assertThat(submitted.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submitted.score()).isNull();

        assertThatThrownBy(() -> submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "again")))
                .isInstanceOf(ConflictException.class);

        SubmissionResponse graded = submissionService.grade(submitted.id(), new GradeRequest(90, "good"));
        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submissions.findById(submitted.id()).orElseThrow().getScore()).isEqualTo(90);   

        assertThat(submissionService.listByAssignment(assignmentId)).hasSize(1);
        assertThat(submissionService.listByStudent(student.id())).extracting("score").containsExactly(90);
    }

    @Test
    void gradeAboveMaximumIsRejected() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));
        SubmissionResponse submitted = submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "x"));

        assertThatThrownBy(() -> submissionService.grade(submitted.id(), new GradeRequest(101, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void studentWithoutEnrollmentCannotSubmit() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));
        UserResponse outsider = newUser(Role.STUDENT);

        assertThatThrownBy(() -> submissionService.submit(assignmentId, new SubmissionRequest(outsider.id(), "x")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void perfectAnswersGiveFullScoreAndPass() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());

        QuizResultResponse result = quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), perfectAnswers(quiz)));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.maxScore()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void wrongAnswersFailTheQuizAndResultsAreStored() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());

        QuizResultResponse result = quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), wrongAnswers(quiz)));

        assertThat(result.score()).isZero();
        assertThat(result.passed()).isFalse();
        assertThat(quizService.resultsByStudent(student.id())).hasSize(1);
        assertThat(quizService.resultsByQuiz(quiz.id())).hasSize(1);
        assertThat(quizService.resultsByCourse(course.course().id())).hasSize(1);
    }

    @Test
    void partialAnswerForMultipleChoiceIsNotCounted() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());
        var q1 = quiz.questions().get(0);
        var q2 = quiz.questions().get(1);

        QuizResultResponse result = quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), Map.of(
                q1.id(), Set.of(q1.options().get(0).id()),
                q2.id(), Set.of(q2.options().get(0).id()))));    

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.percent()).isEqualTo(50);
    }

    @Test
    void moduleCanHaveOnlyOneQuizAndForeignOptionsAreRejected() {
        givenEnrolledStudent();
        Long moduleId = course.modules().get(1).id();
        QuizResponse quiz = newQuiz(moduleId);

        assertThatThrownBy(() -> newQuiz(moduleId)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> quizService.takeQuiz(quiz.id(),
                new TakeQuizRequest(student.id(), Map.of(quiz.questions().get(0).id(), Set.of(-5L)))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void quizAnswerOptionsDoNotExposeCorrectness() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());

        assertThat(QuizResponse.OptionResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName).containsExactly("id", "text");
    }

    @Test
    void reviewRequiresEnrollmentAndIsUnique() {
        givenEnrolledStudent();
        Long courseId = course.course().id();

        reviewService.add(courseId, new ReviewRequest(student.id(), 4, "ok"));

        assertThatThrownBy(() -> reviewService.add(courseId, new ReviewRequest(student.id(), 5, "again")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> reviewService.add(courseId, new ReviewRequest(newUser(Role.STUDENT).id(), 5, "x")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(reviewService.averageRating(courseId)).isEqualTo(4.0);
    }

    @Test
    void courseProgressCountsCompletedLessons() {
        givenEnrolledStudent();
        Long lessonId = firstLessonId(course);

        progressService.completeLesson(lessonId, student.id());

        assertThat(progressService.courseProgress(course.course().id(), student.id()).percent()).isEqualTo(33);
        assertThatThrownBy(() -> progressService.completeLesson(lessonId, student.id()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingStudentRemovesTheirEnrollmentsSubmissionsAndResults() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));
        submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "solution"));
        QuizResponse quiz = newQuiz(course.modules().get(1).id());
        quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), perfectAnswers(quiz)));

        userService.delete(student.id());

        assertThat(enrollmentService.listByCourse(course.course().id())).isEmpty();
        assertThat(submissionService.listByAssignment(assignmentId)).isEmpty();
        assertThat(quizService.resultsByQuiz(quiz.id())).isEmpty();
    }

    @Test
    void teacherWithCoursesCannotBeDeleted() {
        givenEnrolledStudent();

        assertThatThrownBy(() -> userService.delete(teacher.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingQuizRemovesQuestionsAnswersAndResults() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());
        quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), perfectAnswers(quiz)));

        quizService.delete(quiz.id());

        assertThatThrownBy(() -> quizService.get(quiz.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(quizResults.findByStudentIdOrderByTakenAtDesc(student.id())).isEmpty();
    }

    @Test
    void deletingModuleRemovesItsQuizAndResults() {
        givenEnrolledStudent();
        Long moduleId = course.modules().get(1).id();
        QuizResponse quiz = newQuiz(moduleId);
        quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), perfectAnswers(quiz)));

        contentService.deleteModule(moduleId);

        assertThatThrownBy(() -> quizService.get(quiz.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(quizResults.findByStudentIdOrderByTakenAtDesc(student.id())).isEmpty();
        assertThat(courseService.get(course.course().id()).modules()).hasSize(1);
    }

    @Test
    void deletingLessonRemovesItsAssignmentsSubmissionsAndProgress() {
        givenEnrolledStudent();
        Long lessonId = firstLessonId(course);
        Long assignmentId = newAssignment(lessonId);
        submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "s"));
        progressService.completeLesson(lessonId, student.id());

        contentService.deleteLesson(lessonId);

        assertThatThrownBy(() -> assignmentService.get(assignmentId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(submissions.findByStudentIdOrderBySubmittedAtDesc(student.id())).isEmpty();
    }

    @Test
    void deletingAssignmentRemovesItsSubmissions() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));
        submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "s"));

        assignmentService.delete(assignmentId);

        assertThat(submissions.findByStudentIdOrderBySubmittedAtDesc(student.id())).isEmpty();
    }

    @Test
    void nullOrEmptyAnswerSetIsTreatedAsNoAnswer() {
        givenEnrolledStudent();
        QuizResponse quiz = newQuiz(course.modules().get(1).id());
        Map<Long, Set<Long>> answers = new java.util.HashMap<>();
        answers.put(quiz.questions().get(0).id(), null);
        answers.put(quiz.questions().get(1).id(), Set.of());

        QuizResultResponse result = quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), answers));

        assertThat(result.score()).isZero();
    }

    @Test
    void deletingCompletedCourseRemovesEverythingAttachedToIt() {
        givenEnrolledStudent();
        Long assignmentId = newAssignment(firstLessonId(course));
        submissionService.submit(assignmentId, new SubmissionRequest(student.id(), "solution"));
        QuizResponse quiz = newQuiz(course.modules().get(1).id());
        quizService.takeQuiz(quiz.id(), new TakeQuizRequest(student.id(), perfectAnswers(quiz)));
        progressService.completeLesson(firstLessonId(course), student.id());
        reviewService.add(course.course().id(), new ReviewRequest(student.id(), 5, "great"));
        enrollmentService.complete(course.course().id(), student.id());

        courseService.delete(course.course().id());

        assertThatThrownBy(() -> courseService.get(course.course().id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(submissions.findByStudentIdOrderBySubmittedAtDesc(student.id())).isEmpty();
        assertThat(quizResults.findByStudentIdOrderByTakenAtDesc(student.id())).isEmpty();
        assertThat(enrollmentService.listByStudent(student.id())).isEmpty();
        assertThat(userService.get(student.id())).isNotNull();   
    }
}
