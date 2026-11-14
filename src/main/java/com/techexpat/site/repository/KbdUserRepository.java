package com.techexpat.site.repository;

import com.techexpat.site.model.KbdUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbdUserRepository extends JpaRepository<KbdUser, String> {

    Optional<KbdUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<KbdUser> findAllByOrderByCreatedAtDesc();
}
