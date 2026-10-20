package com.techexpat.site.repository;

import com.techexpat.site.model.Receipt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, String> {

    List<Receipt> findByUserIdOrderByUploadedAtDesc(String userId);

    Optional<Receipt> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
