package com.techexpat.site.repository;

import com.techexpat.site.model.KbrUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KbrUserRepository extends JpaRepository<KbrUser, String> {

    Optional<KbrUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
