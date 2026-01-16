package com.gateway.jobs;

import java.io.Serializable;

public class ProcessPaymentJob implements Serializable {
    private String paymentId;
    // We can add more fields if needed, but paymentId is usually enough to look up the DB
    
    // Default constructor for Jackson (JSON)
    public ProcessPaymentJob() {}

    public ProcessPaymentJob(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}