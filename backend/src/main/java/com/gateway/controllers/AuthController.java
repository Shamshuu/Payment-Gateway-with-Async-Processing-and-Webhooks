package com.gateway.controllers;

import com.gateway.models.Merchant;
import com.gateway.repositories.MerchantRepository;
import com.gateway.services.IdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final MerchantRepository merchantRepository;
    private final IdGenerator idGenerator;

    public AuthController(MerchantRepository merchantRepository, IdGenerator idGenerator) {
        this.merchantRepository = merchantRepository;
        this.idGenerator = idGenerator;
    }

    // 1. REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password"); // Get password

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and Password are required"));
        }

        if (merchantRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        Merchant merchant = new Merchant();
        merchant.setId(UUID.randomUUID().toString()); // Use String ID
        merchant.setEmail(email);
        merchant.setPassword(password); // Save Password (Plain text for this demo)
        
        merchant.setApiKey("key_" + idGenerator.generate(""));
        merchant.setApiSecret("secret_" + idGenerator.generate(""));

        merchantRepository.save(merchant);

        return ResponseEntity.ok(Map.of(
            "message", "Merchant created successfully",
            "email", merchant.getEmail(),
            "api_key", merchant.getApiKey(),
            "api_secret", merchant.getApiSecret()
        ));
    }

    // 2. LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        Optional<Merchant> merchantOpt = merchantRepository.findByEmail(email);
        
        // Check if user exists AND password matches
        if (merchantOpt.isPresent()) {
            Merchant m = merchantOpt.get();
            if (m.getPassword().equals(password)) {
                return ResponseEntity.ok(Map.of(
                    "email", m.getEmail(),
                    "api_key", m.getApiKey(),
                    "api_secret", m.getApiSecret()
                ));
            }
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }
}