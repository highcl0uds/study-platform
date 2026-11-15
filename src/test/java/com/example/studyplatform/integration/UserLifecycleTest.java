package com.example.studyplatform.integration;

import com.example.studyplatform.dto.UserRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserLifecycleTest extends IntegrationTestBase {

    @Test
    void emailIsNormalizedAndUniqueIgnoringCase() {
        int n = next();
        UserResponse created = userService.create(new UserRequest("A", "  Mixed" + n + "@Case.IO ", Role.STUDENT, null));

        assertThat(created.email()).isEqualTo("mixed" + n + "@case.io");
        assertThatThrownBy(() -> userService.create(new UserRequest("B", "MIXED" + n + "@case.io", Role.STUDENT, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createsReadsUpdatesAndDeletesUserWithProfile() {
        int n = next();
        var profile = new UserRequest.ProfileData("bio", null, "+7");
        UserResponse created = userService.create(new UserRequest("Name", "u" + n + "@x.io", Role.STUDENT, profile));
        assertThat(userService.get(created.id()).profile().bio()).isEqualTo("bio");

        UserResponse updated = userService.update(created.id(), new UserRequest("New", "u" + n + "@x.io", Role.STUDENT,
                new UserRequest.ProfileData("new bio", "http://a/b.png", null)));
        assertThat(updated.name()).isEqualTo("New");
        assertThat(updated.profile().bio()).isEqualTo("new bio");

        userService.delete(created.id());
        assertThatThrownBy(() -> userService.get(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void teacherWithCoursesCannotBeDemotedToStudent() {
        UserResponse teacher = newUser(Role.TEACHER);
        newCourse(teacher.id());

        assertThatThrownBy(() -> userService.update(teacher.id(),
                new UserRequest(teacher.name(), teacher.email(), Role.STUDENT, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listsUsersByRole() {
        newUser(Role.ADMIN);

        assertThat(userService.list(Role.ADMIN)).isNotEmpty().allMatch(u -> u.role() == Role.ADMIN);
        assertThat(userService.list(null)).hasSizeGreaterThanOrEqualTo(userService.list(Role.ADMIN).size());
    }
}
