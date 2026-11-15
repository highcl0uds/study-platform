package com.example.studyplatform.service;

import com.example.studyplatform.dto.UserRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.Profile;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.UserMapper;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository users;
    private final CourseRepository courses;

    public UserResponse create(UserRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Пользователь с email " + email + " уже существует");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setRole(request.role());
        if (request.profile() != null) {
            user.attachProfile(new Profile());
            applyProfile(user.getProfile(), request.profile());
        }
        users.save(user);
        log.info("Создан пользователь id={} роль={}", user.getId(), user.getRole());
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role) {
        List<User> found = role == null ? users.findAllWithProfile() : users.findAllWithProfileByRole(role);
        return found.stream().map(UserMapper::toResponse).toList();
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = find(id);
        String email = normalize(request.email());
        if (users.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email " + email + " уже используется");
        }
        if (user.getRole() == Role.TEACHER && request.role() != Role.TEACHER && courses.existsByTeacherId(id)) {
            throw new BusinessRuleException("Нельзя сменить роль преподавателя id=" + id + ": у него есть курсы");
        }
        user.setName(request.name());
        user.setEmail(email);
        user.setRole(request.role());
        if (request.profile() != null) {
            if (user.getProfile() == null) {
                user.attachProfile(new Profile());
            }
            applyProfile(user.getProfile(), request.profile());
        }
        log.info("Обновлён пользователь id={}", id);
        return UserMapper.toResponse(user);
    }

    public void delete(Long id) {
        User user = find(id);
        if (courses.existsByTeacherId(id)) {
            throw new ConflictException("Нельзя удалить преподавателя id=" + id + ": у него есть курсы");
        }
        users.delete(user);
        log.info("Удалён пользователь id={}", id);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private User find(Long id) {
        return users.findWithProfileById(id).orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
    }

    private void applyProfile(Profile profile, UserRequest.ProfileData data) {
        profile.setBio(data.bio());
        profile.setAvatarUrl(data.avatarUrl());
        profile.setPhone(data.phone());
    }
}
