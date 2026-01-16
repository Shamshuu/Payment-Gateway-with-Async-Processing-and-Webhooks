package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entities.Refund;
import com.gateway.entities.WebhookLog;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.jobs.ProcessRefundJob;
import com.gateway.repositories.RefundRepository;
import com.gateway.repositories.WebhookLogRepository; // Ensure this import exists
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefundWorker implements MessageListener {

    private final RefundRepository refundRepository;
    private final WebhookLogRepository webhookLogRepository; // Added
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Updated Constructor
    public RefundWorker(RefundRepository refundRepository,
            WebhookLogRepository webhookLogRepository,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.refundRepository = refundRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1. Deserialize
            ProcessRefundJob job = objectMapper.readValue(message.getBody(), ProcessRefundJob.class);
            System.out.println("RefundWorker Received: " + job.getRefundId());

            // 2. Fetch Refund
            Optional<Refund> refundOpt = refundRepository.findById(job.getRefundId());
            if (refundOpt.isEmpty())
                return;
            Refund refund = refundOpt.get();

            // 3. Process with random 3-5 second delay
            long delay = 3000 + (long) (Math.random() * 2000);
            Thread.sleep(delay);
            refund.setStatus("processed");
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);

            System.out.println("Refund PROCESSED: " + refund.getId());

            // 4. Create Webhook Log (The Fix)
            try {
                WebhookLog log = new WebhookLog();
                log.setId(UUID.randomUUID());
                log.setMerchantId(refund.getMerchantId());
                log.setEvent("refund.processed");
                // Convert payload to JSON String
                log.setPayload(objectMapper.valueToTree(refund).toString());
                log.setStatus("pending");
                log.setAttempts(0);
                log.setCreatedAt(LocalDateTime.now());

                webhookLogRepository.save(log);

                // 5. Enqueue Job with LOG ID only
                // Use log.getId().toString() if your Job expects a String
                DeliverWebhookJob webhookJob = new DeliverWebhookJob(log.getId().toString());
                redisTemplate.convertAndSend("queue:webhooks", webhookJob);

            } catch (Exception e) {
                System.out.println("Failed to create webhook log: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}