package com.microform.webhook.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class DefaultWebhookHttpClient implements WebhookHttpClient {

    private final HttpClient client;
    private final Duration requestTimeout;

    public DefaultWebhookHttpClient(WebhookProperties props) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
        this.requestTimeout = Duration.ofMillis(props.getRequestTimeoutMs());
    }

    @Override
    public int post(String url, String payloadJson, String signatureHeader) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "microform-webhook/1.0");
            if (signatureHeader != null) {
                builder.header("X-Microform-Signature", signatureHeader);
            }
            HttpRequest req = builder.POST(HttpRequest.BodyPublishers.ofString(payloadJson)).build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode();
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("webhook delivery failed: " + e.getMessage(), e);
        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "microform.webhook")
    public static class WebhookProperties {
        private long connectTimeoutMs = 3000;
        private long requestTimeoutMs = 5000;
        private int maxAttempts = 5;
        private long retryBaseMs = 1000;
        private long retryMaxMs = 60_000;
        private int batchSize = 50;
        private long pollIntervalMs = 2000;
        private boolean schedulerEnabled = true;

        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long v) { this.connectTimeoutMs = v; }
        public long getRequestTimeoutMs() { return requestTimeoutMs; }
        public void setRequestTimeoutMs(long v) { this.requestTimeoutMs = v; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public long getRetryBaseMs() { return retryBaseMs; }
        public void setRetryBaseMs(long v) { this.retryBaseMs = v; }
        public long getRetryMaxMs() { return retryMaxMs; }
        public void setRetryMaxMs(long v) { this.retryMaxMs = v; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int v) { this.batchSize = v; }
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long v) { this.pollIntervalMs = v; }
        public boolean isSchedulerEnabled() { return schedulerEnabled; }
        public void setSchedulerEnabled(boolean v) { this.schedulerEnabled = v; }
    }
}
