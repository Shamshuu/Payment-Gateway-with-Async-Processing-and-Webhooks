package com.gateway.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gateway.entities.WebhookLog;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {
    ArrayList<WebhookLog> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<WebhookLog> findByStatusAndNextRetryAtLessThanEqual(String status, LocalDateTime timestamp);
}