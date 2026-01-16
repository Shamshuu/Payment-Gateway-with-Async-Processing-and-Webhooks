package com.gateway.workers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.jobs.DeliverWebhookJob;
import com.gateway.jobs.ProcessPaymentJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JobQueueListener implements MessageListener {

    @Autowired private WebhookWorker webhookWorker;
    @Autowired private PaymentWorker paymentWorker;
    
    // --- FIX: Configure Mapper to ignore unknown fields like "@class" ---
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            
            // Debug Print (Optional: Helps prove it arrived)
            System.out.println("JobQueueListener Received: " + body);

            // 1. Route based on the channel (queue name)
            if ("queue:webhooks".equals(channel)) {
                DeliverWebhookJob job = objectMapper.readValue(body, DeliverWebhookJob.class);
                webhookWorker.process(job);
                
            } else if ("queue:payments".equals(channel)) {
                ProcessPaymentJob job = objectMapper.readValue(body, ProcessPaymentJob.class);
                paymentWorker.process(job);
            }

        } catch (Exception e) {
            System.err.println("Error processing job from Redis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}