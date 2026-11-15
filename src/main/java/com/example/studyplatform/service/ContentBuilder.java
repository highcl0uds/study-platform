package com.example.studyplatform.service;

import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;

import java.util.List;

final class ContentBuilder {

    private ContentBuilder() {
    }

    static Module module(ModuleRequest request) {
        Module module = new Module();
        applyModule(module, request);
        List<LessonRequest> lessons = request.lessons();
        if (lessons != null) {
            int next = 0;
            for (LessonRequest lessonRequest : lessons) {
                Lesson lesson = new Lesson();
                applyLesson(lesson, lessonRequest, next++);
                module.addLesson(lesson);
            }
        }
        return module;
    }

    static void applyModule(Module module, ModuleRequest request) {
        module.setTitle(request.title());
        module.setOrderIndex(request.orderIndex());
        module.setDescription(request.description());
    }

    static void applyLesson(Lesson lesson, LessonRequest request, int defaultOrder) {
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setOrderIndex(request.orderIndex() != null ? request.orderIndex() : defaultOrder);
    }
}
