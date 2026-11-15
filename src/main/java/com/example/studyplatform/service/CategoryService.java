package com.example.studyplatform.service;

import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.dto.NameResponse;
import com.example.studyplatform.entity.Category;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.mapper.CourseMapper;
import com.example.studyplatform.repository.CategoryRepository;
import com.example.studyplatform.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categories;
    private final CourseRepository courses;
    private final EntityLookup lookup;

    public NameResponse create(NameRequest request) {
        if (categories.existsByName(request.name())) {
            throw new ConflictException("Категория '" + request.name() + "' уже существует");
        }
        Category category = new Category();
        category.setName(request.name());
        categories.save(category);
        log.info("Создана категория id={}", category.getId());
        return CourseMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public NameResponse get(Long id) {
        return CourseMapper.toResponse(lookup.category(id));
    }

    @Transactional(readOnly = true)
    public List<NameResponse> list() {
        return categories.findAll().stream().map(CourseMapper::toResponse).toList();
    }

    public NameResponse update(Long id, NameRequest request) {
        Category category = lookup.category(id);
        if (categories.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Категория '" + request.name() + "' уже существует");
        }
        category.setName(request.name());
        return CourseMapper.toResponse(category);
    }

    public void delete(Long id) {
        Category category = lookup.category(id);
        if (courses.existsByCategoryId(id)) {
            throw new ConflictException("Нельзя удалить категорию id=" + id + ": в ней есть курсы");
        }
        categories.delete(category);
        log.info("Удалена категория id={}", id);
    }
}
