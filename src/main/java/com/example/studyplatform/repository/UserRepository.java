package com.example.studyplatform.repository;

import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("select u from User u left join fetch u.profile order by u.id")
    List<User> findAllWithProfile();

    @Query("select u from User u left join fetch u.profile where u.role = :role order by u.id")
    List<User> findAllWithProfileByRole(@Param("role") Role role);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findByRole(Role role);

    /** JOIN FETCH профиля предотвращает дополнительные запросы для инверсной optional-связи 1-1. */
    @Query("select u from User u left join fetch u.profile where u.id = :id")
    Optional<User> findWithProfileById(@Param("id") Long id);
}
