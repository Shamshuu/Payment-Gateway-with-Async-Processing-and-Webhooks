package com.gateway.controllers;

import com.gateway.entities.Refund;
import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import com.gateway.repositories.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/refunds") // Maps to /api/v1/refunds
public class RefundController {

    private final RefundRepository refundRepository;
    private final MerchantRepository merchantRepository;

    public RefundController(RefundRepository refundRepository, MerchantRepository merchantRepository) {
        this.refundRepository = refundRepository;
        this.merchantRepository = merchantRepository;
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<?> getRefund(
            @RequestHeader("X-Api-Key") String apiKey, 
            @PathVariable String refundId) {

        // 1. Authenticate
        Merchant merchant = merchantRepository.findAll().stream()
                .filter(m -> m.getApiKey().equals(apiKey))
                .findFirst()
                .orElse(null);

        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid API Key"));
        }

        // 2. Fetch Refund
        Optional<Refund> refund = refundRepository.findById(refundId);
        if (refund.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 3. Verify Ownership (Optional but recommended)
        // Ensure the refund belongs to the merchant asking for it
        if (!refund.get().getMerchantId().toString().equals(merchant.getId())) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(refund.get());
    }
}