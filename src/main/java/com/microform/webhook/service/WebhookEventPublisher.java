package com.microform.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microform.webhook.domain.DeliveryStatus;
import com.microform.webhook.domain.WebhookDelivery;
import com.microform.webhook.domain.WebhookEventType;
import com.microform.webhook.persistence.WebhookDeliveryRepository;
import com.microform.webhook.persistence.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WebhookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventPublisher.class);

    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookDeliveryRepository deliveries;
    private final ObjectMapper mapper;
    private final DefaultWebhookHttpClient.WebhookProperties props;

    public WebhookEventPublisher(WebhookSubscriptionRepository subscriptions,
                                 WebhookDeliveryRepository deliveries,
                                 ObjectMapper mapper,
                                 DefaultWebhookHttpClient.WebhookProperties props) {
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public void publish(WebhookEventType type, Map<String, Object> body) {
        var subs = subscriptions.findActiveForEvent(type);
        if (subs.isEmpty()) {
            log.debug("No active subscribers for {}", type);
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event", type.name());
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("data", body);
        String payload;
        try {
            payload = mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize webhook payload", e);
        }
        for (var sub : subs) {
            WebhookDelivery d = new WebhookDelivery();
            d.setSubscriptionId(sub.getId());
            d.setEventType(type);
            d.setPayloadJson(payload);
            d.setStatus(DeliveryStatus.PENDING);
            d.setAttempts(0);
            d.setMaxAttempts(props.getMaxAttempts());
            d.setNextAttemptAt(Instant.now());
            deliveries.insert(d);
            log.debug("Enqueued delivery {} for sub {} event {}", d.getId(), sub.getId(), type);
        }
    }
}
