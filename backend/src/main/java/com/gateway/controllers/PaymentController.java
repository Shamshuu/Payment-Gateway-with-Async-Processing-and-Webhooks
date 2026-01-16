package com.gateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.dto.PaymentRequest;
import com.gateway.entities.IdempotencyKey;
import com.gateway.entities.IdempotencyKeyId;
import com.gateway.entities.Refund;
import com.gateway.entities.WebhookLog;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.jobs.ProcessPaymentJob;
import com.gateway.jobs.ProcessRefundJob;
import com.gateway.models.Merchant;
import com.gateway.models.Payment;
import com.gateway.repositories.IdempotencyKeyRepository;
import com.gateway.repositories.MerchantRepository;
import com.gateway.repositories.PaymentRepository;
import com.gateway.repositories.RefundRepository;
import com.gateway.repositories.WebhookLogRepository;
import com.gateway.services.PaymentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService,
            MerchantRepository merchantRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            WebhookLogRepository webhookLogRepository,
            RedisTemplate<String, Object> redisTemplate,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.redisTemplate = redisTemplate;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> createPaymentAsync(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestBody PaymentRequest request) {

        // 1. Authenticate Merchant
        List<Merchant> allMerchants = merchantRepository.findAll();
        Merchant merchant = allMerchants.stream()
                .filter(m -> m.getApiKey().equals(apiKey))
                .findFirst()
                .orElse(null);

        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid API Key"));
        }

        UUID merchantUuid = UUID.fromString(merchant.getId());

        // 2. IDEMPOTENCY CHECK
        if (idempotencyKeyHeader != null) {
            IdempotencyKeyId id = new IdempotencyKeyId(idempotencyKeyHeader, merchantUuid);
            Optional<IdempotencyKey> cachedKey = idempotencyKeyRepository.findById(id);

            if (cachedKey.isPresent()) {
                if (cachedKey.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                    try {
                        Object cachedResponse = objectMapper.readValue(cachedKey.get().getResponse(), Object.class);
                        return ResponseEntity.status(HttpStatus.CREATED).body(cachedResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    idempotencyKeyRepository.delete(cachedKey.get());
                }
            }
        }

        // 3. Process Payment
        Payment payment = new Payment();
        payment.setId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        payment.setMerchantId(merchant.getId());
        payment.setOrderId(request.getOrderId()); // Set orderId from request
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setVpa(request.getVpa());
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Emit payment.created webhook
        createWebhookLog(merchantUuid, "payment.created", payment);

        ProcessPaymentJob job = new ProcessPaymentJob(payment.getId());
        redisTemplate.convertAndSend("queue:payments", job);

        // 4. SAVE IDEMPOTENCY KEY
        if (idempotencyKeyHeader != null) {
            try {
                IdempotencyKey newKey = new IdempotencyKey();
                newKey.setKey(idempotencyKeyHeader);
                newKey.setMerchantId(merchantUuid);
                newKey.setResponse(objectMapper.writeValueAsString(payment));
                newKey.setExpiresAt(LocalDateTime.now().plusHours(24));
                idempotencyKeyRepository.save(newKey);
            } catch (Exception e) {
                System.out.println(">>> Failed to save idempotency key: " + e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    // --- REFUND ENDPOINT ---
    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<?> createRefund(
            @RequestHeader("X-Api-Key") String apiKey,
            @PathVariable String paymentId,
            @RequestBody Map<String, Object> request) {

        // 1. Authenticate
        Merchant merchant = merchantRepository.findAll().stream()
                .filter(m -> m.getApiKey().equals(apiKey))
                .findFirst()
                .orElse(null);

        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid API Key"));
        }

        // 2. Fetch Payment
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Payment not found"));
        }
        Payment payment = paymentOpt.get();

        // 3. Verify Ownership & Status
        if (!String.valueOf(payment.getMerchantId()).equals(String.valueOf(merchant.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        // Check payment can be refunded
        if (!"success".equals(payment.getStatus()) && !"captured".equals(payment.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    Map.of("code", "BAD_REQUEST_ERROR", "description", "Payment is not in a refundable state")));
        }

        if (payment.getAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    Map.of("code", "BAD_REQUEST_ERROR", "description", "Payment amount is invalid")));
        }

        // 4. Validate Amount
        Integer requestAmount = (Integer) request.get("amount");
        Integer alreadyRefunded = refundRepository.getRefundedAmount(paymentId);
        if (alreadyRefunded == null) {
            alreadyRefunded = 0;
        }

        if (requestAmount + alreadyRefunded > payment.getAmount()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    Map.of("code", "BAD_REQUEST_ERROR", "description", "Refund amount exceeds available amount")));
        }

        // 5. Create Refund
        Refund refund = new Refund();
        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        refund.setId(refundId);
        refund.setPaymentId(paymentId);
        refund.setMerchantId(UUID.fromString(merchant.getId()));
        refund.setAmount(requestAmount);
        refund.setReason((String) request.get("reason"));
        refund.setStatus("pending");
        refund.setCreatedAt(LocalDateTime.now());

        refundRepository.save(refund);

        // Emit refund.created webhook
        createWebhookLog(UUID.fromString(merchant.getId()), "refund.created", refund);

        // 6. Enqueue Job
        ProcessRefundJob job = new ProcessRefundJob(refundId);
        redisTemplate.convertAndSend("queue:refunds", job);

        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    @PostMapping("/public")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.ok(Map.of("id", payment.getId(), "status", payment.getStatus()));
    }

    @GetMapping("/{id}/public")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(Map.of("id", payment.getId(), "status", payment.getStatus()));
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<?> capturePayment(
            @RequestHeader("X-Api-Key") String apiKey,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {

        Merchant merchant = merchantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Invalid API Key"));

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getMerchantId().equals(merchant.getId())) {
            return ResponseEntity.status(404).body(Map.of("error",
                    Map.of("code", "NOT_FOUND_ERROR", "description", "Payment not found")));
        }

        if (!"success".equals(payment.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    Map.of("code", "BAD_REQUEST_ERROR", "description", "Payment is not in a capturable state")));
        }

        payment.setCaptured(true);
        paymentRepository.save(payment);

        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public ResponseEntity<?> getAllPayments(@RequestHeader("X-Api-Key") String apiKey) {
        Merchant merchant = merchantRepository.findAll().stream()
                .filter(m -> m.getApiKey().equals(apiKey))
                .findFirst()
                .orElse(null);

        if (merchant == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid API Key"));
        }

        List<Payment> payments = paymentService.getPaymentsForMerchant(merchant.getId());
        return ResponseEntity.ok(payments);
    }

    // Helper method to create webhook log and enqueue delivery
    private void createWebhookLog(UUID merchantId, String event, Object data) {
        try {
            WebhookLog log = new WebhookLog();
            log.setId(UUID.randomUUID());
            log.setMerchantId(merchantId);
            log.setEvent(event);

            Map<String, Object> payloadMap = Map.of(
                    "event", event,
                    "timestamp", System.currentTimeMillis() / 1000,
                    "data", data);
            log.setPayload(objectMapper.writeValueAsString(payloadMap));
            log.setStatus("pending");
            log.setAttempts(0);
            log.setCreatedAt(LocalDateTime.now());

            webhookLogRepository.save(log);

            DeliverWebhookJob webhookJob = new DeliverWebhookJob(log.getId().toString());
            redisTemplate.convertAndSend("queue:webhooks", webhookJob);
        } catch (Exception e) {
            System.out.println("Failed to create webhook log: " + e.getMessage());
        }
    }

}