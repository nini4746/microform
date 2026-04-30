package com.microform.webhook.service;

import com.microform.webhook.domain.DeliveryStatus;
import com.microform.webhook.domain.WebhookDelivery;
import com.microform.webhook.persistence.WebhookDeliveryRepository;
import com.microform.webhook.persistence.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookDeliveryRepository deliveries;
    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookHttpClient http;
    private final WebhookSignatureService signature;
    private final DefaultWebhookHttpClient.WebhookProperties props;

    public WebhookDispatcher(WebhookDeliveryRepository deliveries,
                             WebhookSubscriptionRepository subscriptions,
                             WebhookHttpClient http,
                             WebhookSignatureService signature,
                             DefaultWebhookHttpClient.WebhookProperties props) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
        this.http = http;
        this.signature = signature;
        this.props = props;
    }

    /**
     * Dispatch up to batchSize due pending deliveries. Returns the number of deliveries attempted.
     */
    @Transactional
    public int dispatchBatch() {
        List<WebhookDelivery> due = deliveries.findDuePending(Instant.now(), props.getBatchSize());
        for (WebhookDelivery d : due) {
            attempt(d);
        }
        return due.size();
    }

    @Transactional
    public void attempt(WebhookDelivery d) {
        var subOpt = subscriptions.findById(d.getSubscriptionId());
        if (subOpt.isEmpty() || !subOpt.get().isActive()) {
            d.setStatus(DeliveryStatus.DEAD_LETTER);
            d.setLastError("subscription missing or inactive");
            deliveries.update(d);
            return;
        }
        var sub = subOpt.get();
        d.setAttempts(d.getAttempts() + 1);
        try {
            String sig = signature.sign(sub.getSecret(), d.getPayloadJson());
            int status = http.post(sub.getUrl(), d.getPayloadJson(), sig);
            d.setResponseStatus(status);
            if (status >= 200 && status < 300) {
                d.setStatus(DeliveryStatus.SUCCEEDED);
                d.setLastError(null);
                d.setNextAttemptAt(null);
            } else {
                fail(d, "HTTP " + status);
            }
        } catch (RuntimeException ex) {
            fail(d, truncate(ex.getMessage()));
        }
        deliveries.update(d);
    }

    private void fail(WebhookDelivery d, String error) {
        d.setLastError(error);
        if (d.getAttempts() >= d.getMaxAttempts()) {
            d.setStatus(DeliveryStatus.DEAD_LETTER);
            d.setNextAttemptAt(null);
            log.warn("Delivery {} moved to DEAD_LETTER after {} attempts: {}", d.getId(), d.getAttempts(), error);
        } else {
            d.setStatus(DeliveryStatus.PENDING);
            long delayMs = nextDelayMs(d.getAttempts());
            d.setNextAttemptAt(Instant.now().plus(Duration.ofMillis(delayMs)));
            log.info("Delivery {} attempt {} failed: {} -> retry in {}ms", d.getId(), d.getAttempts(), error, delayMs);
        }
    }

    public long nextDelayMs(int attempt) {
        long base = props.getRetryBaseMs();
        long max = props.getRetryMaxMs();
        // exponential backoff: base * 2^(attempt-1), capped
        long shift = Math.min(attempt - 1, 30);
        long calc = base * (1L << shift);
        return Math.min(Math.max(calc, base), max);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 990 ? s.substring(0, 990) : s;
    }
}
