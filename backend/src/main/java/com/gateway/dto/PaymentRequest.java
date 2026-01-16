package com.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentRequest {

    // --- NEW FIELDS ADDED ---
    private Long amount;    // Required by Controller
    private String currency; // Required by Controller

    @JsonProperty("order_id")
    private String orderId;

    private String method; // "upi" or "card"
    
    private String vpa; // For UPI
    private String email; 

    // --- Getters and Setters ---

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}