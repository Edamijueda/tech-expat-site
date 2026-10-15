package com.techexpat.site.repository;

import com.techexpat.site.model.KbrUserLockoutEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbrUserLockoutEventRepository extends JpaRepository<KbrUserLockoutEvent, String> {

    List<KbrUserLockoutEvent> findByUserIdOrderByLockedAtDesc(String userId);

    void deleteByUserId(String userId);
}
