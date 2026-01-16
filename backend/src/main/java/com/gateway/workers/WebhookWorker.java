package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entities.WebhookLog;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import com.gateway.repositories.WebhookLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener; // MUST be imported
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class WebhookWorker implements MessageListener {

    private final WebhookLogRepository webhookLogRepository;
    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${WEBHOOK_RETRY_INTERVALS_TEST:false}")
    private boolean testMode;

    public WebhookWorker(WebhookLogRepository webhookLogRepository,
            MerchantRepository merchantRepository,
            ObjectMapper objectMapper) {
        this.webhookLogRepository = webhookLogRepository;
        this.merchantRepository = merchantRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    // --- THIS IS THE CRITICAL METHOD ---
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            DeliverWebhookJob job = objectMapper.readValue(message.getBody(), DeliverWebhookJob.class);
            process(job);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void process(DeliverWebhookJob job) {
        WebhookLog log = webhookLogRepository.findById(UUID.fromString(job.getWebhookLogId())).orElse(null);
        if (log == null || "success".equals(log.getStatus()))
            return;

        try {
            // Fix: Convert Merchant ID (String) to UUID for lookup if needed, or stick to
            // String if Repo expects it
            // Based on your MerchantRepository error earlier, it expects String ID.
            Merchant merchant = merchantRepository.findById(log.getMerchantId().toString()).orElseThrow();

            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isEmpty()) {
                log.setStatus("failed");
                webhookLogRepository.save(log);
                return;
            }

            String jsonPayload = log.getPayload();
            String signature = generateSignature(jsonPayload, merchant.getWebhookSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            restTemplate.postForEntity(merchant.getWebhookUrl(), request, String.class);

            log.setStatus("success");
            log.setResponseCode(200);
            log.setLastAttemptAt(LocalDateTime.now());
            webhookLogRepository.save(log);

        } catch (Exception e) {
            log.setAttempts(log.getAttempts() + 1);
            log.setResponseBody(
                    e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 255))
                            : "Error");

            if (log.getAttempts() >= 5) {
                log.setStatus("failed");
                log.setNextRetryAt(null);
            } else {
                log.setStatus("pending");
                // Calculate next retry time
                long delaySeconds = 0;
                if (testMode) {
                    // Test Mode Intervals: 0s (immediate, already done), 5s, 10s, 15s, 20s
                    // Note: Attempts passed here is AFTER increment. So attempt 1 failed ->
                    // attempts=1. Next is attempt 2.
                    // Logic:
                    // Attempt 1 (initial): failed. attempts=1. Next retry (for attempt 2) in 5s.
                    // Attempt 2: failed. attempts=2. Next retry (for attempt 3) in 10s.
                    switch (log.getAttempts()) {
                        case 1:
                            delaySeconds = 5;
                            break;
                        case 2:
                            delaySeconds = 10;
                            break;
                        case 3:
                            delaySeconds = 15;
                            break;
                        case 4:
                            delaySeconds = 20;
                            break;
                    }
                } else {
                    // Production Intervals: 1m, 5m, 30m, 2h
                    // Attempt 1 failed. Next (Att 2) in 1 min.
                    // Att 2 failed. Next (Att 3) in 5 min.
                    switch (log.getAttempts()) {
                        case 1:
                            delaySeconds = 60;
                            break;
                        case 2:
                            delaySeconds = 300;
                            break;
                        case 3:
                            delaySeconds = 1800;
                            break;
                        case 4:
                            delaySeconds = 7200;
                            break;
                    }
                }
                log.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            }

            webhookLogRepository.save(log);
        }
    }

    private String generateSignature(String data, String secret) {
        try {
            if (secret == null)
                return "";
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
}