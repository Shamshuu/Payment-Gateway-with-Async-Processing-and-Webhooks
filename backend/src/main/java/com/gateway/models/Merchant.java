package com.gateway.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Data // Lombok handles getters/setters automatically
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    private String id;

    @Column(nullable = false)
    private String name = "Merchant"; 

    @Column(nullable = false, unique = true)
    private String email;

    // --- ADD THIS FIELD ---
    @Column(nullable = false)
    private String password; 
    // ----------------------

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "api_secret", nullable = false, length = 64)
    private String apiSecret;

    @Column(name = "webhook_url", columnDefinition = "TEXT")
    private String webhookUrl;

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "webhook_secret")
    private String webhookSecret;
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}