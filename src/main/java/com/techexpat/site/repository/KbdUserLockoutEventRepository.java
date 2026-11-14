package com.techexpat.site.repository;

import com.techexpat.site.model.KbdUserLockoutEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbdUserLockoutEventRepository extends JpaRepository<KbdUserLockoutEvent, String> {

    List<KbdUserLockoutEvent> findByUserIdOrderByLockedAtDesc(String userId);

    void deleteByUserId(String userId);
}
