package com.gateway.controllers;

import com.gateway.entities.WebhookLog;
import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import com.gateway.repositories.WebhookLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookLogRepository webhookLogRepository;
    private final MerchantRepository merchantRepository;

    public WebhookController(WebhookLogRepository webhookLogRepository, MerchantRepository merchantRepository) {
        this.webhookLogRepository = webhookLogRepository;
        this.merchantRepository = merchantRepository;
    }

    @GetMapping
    public ResponseEntity<?> getLogs(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        // 1. Authenticate
        Merchant merchant = merchantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        // 2. Fetch All Logs for merchant
        List<WebhookLog> allLogs = webhookLogRepository
                .findByMerchantIdOrderByCreatedAtDesc(UUID.fromString(merchant.getId()));
        int total = allLogs.size();

        // 3. Apply Offset and Limit
        int end = Math.min(offset + limit, total);
        List<WebhookLog> pagedLogs = offset < total ? allLogs.subList(offset, end) : List.of();

        return ResponseEntity.ok(Map.of(
                "data", pagedLogs,
                "total", total,
                "limit", limit,
                "offset", offset));
    }

    @PostMapping("/{logId}/retry")
    public ResponseEntity<?> retryWebhook(
            @RequestHeader("X-Api-Key") String apiKey,
            @PathVariable UUID logId) {

        // 1. Find the log
        WebhookLog log = webhookLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));

        // 2. Reset status to trigger retry
        log.setStatus("pending");
        log.setAttempts(0);
        webhookLogRepository.save(log);

        return ResponseEntity.ok(Map.of(
                "id", log.getId().toString(),
                "status", "pending",
                "message", "Retry scheduled"));
    }
}