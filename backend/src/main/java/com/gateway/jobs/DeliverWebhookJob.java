package com.gateway.jobs;

import java.io.Serializable;

public class DeliverWebhookJob implements Serializable {
    private String webhookLogId; // We track the log ID to manage retries

    public DeliverWebhookJob() {}

    public DeliverWebhookJob(String webhookLogId) {
        this.webhookLogId = webhookLogId;
    }

    public String getWebhookLogId() {
        return webhookLogId;
    }
}