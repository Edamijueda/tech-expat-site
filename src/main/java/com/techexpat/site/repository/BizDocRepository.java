package com.techexpat.site.repository;

import com.techexpat.site.model.BizDoc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BizDocRepository extends JpaRepository<BizDoc, String> {

    List<BizDoc> findByUserIdOrderByUploadedAtDesc(String userId);

    Optional<BizDoc> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
