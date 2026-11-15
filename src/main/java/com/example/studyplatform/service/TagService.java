package com.example.studyplatform.service;

import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.dto.NameResponse;
import com.example.studyplatform.entity.Tag;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.CourseMapper;
import com.example.studyplatform.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TagService {

    private final TagRepository tags;

    public NameResponse create(NameRequest request) {
        if (tags.existsByName(request.name())) {
            throw new ConflictException("Тег '" + request.name() + "' уже существует");
        }
        Tag tag = new Tag();
        tag.setName(request.name());
        tags.save(tag);
        log.info("Создан тег id={}", tag.getId());
        return CourseMapper.toResponse(tag);
    }

    @Transactional(readOnly = true)
    public List<NameResponse> list() {
        return tags.findAll().stream().map(CourseMapper::toResponse).toList();
    }

    /** Связи в course_tag удаляются вместе с тегом через сторону-владельца: сначала отвязываем курсы. */
    public void delete(Long id) {
        Tag tag = tags.findById(id).orElseThrow(() -> new ResourceNotFoundException("Тег", id));
        tag.getCourses().forEach(course -> course.getTags().remove(tag));
        tags.delete(tag);
        log.info("Удалён тег id={}", id);
    }
}
