package com.gateway.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.entities.WebhookLog;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.models.Payment;
import com.gateway.repositories.PaymentRepository;
import com.gateway.repositories.WebhookLogRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentWorker implements MessageListener {

    private final PaymentRepository paymentRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${TEST_MODE:false}")
    private boolean testMode;

    @org.springframework.beans.factory.annotation.Value("${TEST_PAYMENT_SUCCESS:true}")
    private boolean testPaymentSuccess;

    public PaymentWorker(PaymentRepository paymentRepository,
            WebhookLogRepository webhookLogRepository,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1. Deserialize the job from Redis
            ProcessPaymentJob job = objectMapper.readValue(message.getBody(), ProcessPaymentJob.class);

            // 2. Delegate to the process method (This fixes your error)
            process(job);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- The Missing Method ---
    public void process(ProcessPaymentJob job) {
        try {
            System.out.println("Processing Payment: " + job.getPaymentId());

            // 1. Fetch Payment
            Optional<Payment> paymentOpt = paymentRepository.findById(job.getPaymentId());
            if (paymentOpt.isEmpty())
                return;
            Payment payment = paymentOpt.get();

            // 2. Simulate Bank Processing
            long delay = testMode ? 1000 : (5000 + (long) (Math.random() * 5000));
            Thread.sleep(delay);

            // 3. Update Status (Random Success/Fail)
            // UPI: 90% success, Card: 95% success
            double threshold = "upi".equalsIgnoreCase(payment.getMethod()) ? 0.90 : 0.95;

            // Override with Test Environment Variables if set
            // (Assuming environment variables are being read into fields in the class,
            // but for now let's stick to the core logic requested or use simple hardcoded
            // Logic if Env vars aren't wired up yet in this worker class)
            // The prompt mentioned TEST_MODE triggers specific outcomes.
            // We should inject @Value("${TEST_MODE:false}") private boolean testMode;
            // And maybe @Value("${TEST_PAYMENT_SUCCESS:true}") private boolean
            // testPaymentSuccess;

            boolean success;
            if (testMode) {
                success = testPaymentSuccess; // Use env var for test control
            } else {
                success = Math.random() < threshold;
            }

            payment.setStatus(success ? "success" : "failed");
            if (!success) {
                payment.setErrorCode("PAYMENT_FAILED");
                payment.setErrorDescription("Transaction declined by bank");
            }

            paymentRepository.save(payment);
            System.out.println("Payment " + payment.getStatus().toUpperCase() + ": " + payment.getId());

            // 4. Create Webhook Log
            createWebhookLog(payment, success);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createWebhookLog(Payment payment, boolean success) {
        try {
            WebhookLog log = new WebhookLog();
            log.setId(UUID.randomUUID());
            // Convert String ID to UUID
            log.setMerchantId(UUID.fromString(payment.getMerchantId()));
            log.setEvent(success ? "payment.success" : "payment.failed");

            // Create Payload
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("event", log.getEvent());
            payloadMap.put("timestamp", System.currentTimeMillis() / 1000);
            payloadMap.put("data", Map.of("payment", payment));

            // Convert Map to JSON String
            log.setPayload(objectMapper.writeValueAsString(payloadMap));

            log.setStatus("pending");
            log.setAttempts(0);
            log.setCreatedAt(LocalDateTime.now());

            webhookLogRepository.save(log);

            // Enqueue Webhook Job
            DeliverWebhookJob webhookJob = new DeliverWebhookJob(log.getId().toString());
            redisTemplate.convertAndSend("queue:webhooks", webhookJob);

        } catch (Exception e) {
            System.out.println("Failed to enqueue webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }
}