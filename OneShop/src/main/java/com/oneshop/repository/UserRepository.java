package com.oneshop.repository;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRoleAndUsernameIn(Role role, List<String> usernames);
    List<User> findByUsernameIn(List<String> usernames);
}