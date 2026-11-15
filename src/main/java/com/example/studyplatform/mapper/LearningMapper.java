package com.example.studyplatform.mapper;

import com.example.studyplatform.dto.EnrollmentResponse;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.ReviewResponse;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.entity.CourseReview;
import com.example.studyplatform.entity.Enrollment;
import com.example.studyplatform.entity.Question;
import com.example.studyplatform.entity.Quiz;
import com.example.studyplatform.entity.QuizSubmission;
import com.example.studyplatform.entity.Submission;

import java.util.List;

public final class LearningMapper {

    private LearningMapper() {
    }

    public static EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getStudent().getId(), e.getStudent().getName(),
                e.getCourse().getId(), e.getCourse().getTitle(), e.getEnrollDate(), e.getStatus());
    }

    public static SubmissionResponse toResponse(Submission s) {
        return new SubmissionResponse(s.getId(), s.getAssignment().getId(), s.getAssignment().getTitle(),
                s.getStudent().getId(), s.getStudent().getName(), s.getSubmittedAt(), s.getContent(),
                s.getScore(), s.getFeedback(), s.getStatus());
    }

    public static QuizResultResponse toResponse(QuizSubmission q) {
        return new QuizResultResponse(q.getId(), q.getQuiz().getId(), q.getQuiz().getTitle(),
                q.getStudent().getId(), q.getStudent().getName(), q.getScore(), q.getMaxScore(),
                q.getPercent(), q.isPassed(), q.getTakenAt());
    }

    public static ReviewResponse toResponse(CourseReview r) {
        return new ReviewResponse(r.getId(), r.getCourse().getId(), r.getStudent().getId(),
                r.getStudent().getName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }

    public static QuizResponse toResponse(Quiz quiz, List<Question> questions) {
        List<QuizResponse.QuestionResponse> qs = questions.stream()
                .map(q -> new QuizResponse.QuestionResponse(q.getId(), q.getText(), q.getType(),
                        q.getOptions().stream()
                                .map(o -> new QuizResponse.OptionResponse(o.getId(), o.getText()))
                                .toList()))
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getModule().getId(), quiz.getTitle(),
                quiz.getTimeLimit(), quiz.getPassingPercent(), qs);
    }
}
