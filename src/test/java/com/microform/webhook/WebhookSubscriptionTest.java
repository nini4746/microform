package com.microform.webhook;

import com.microform.webhook.domain.WebhookEventType;
import com.microform.webhook.domain.WebhookSubscription;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSubscriptionTest {

    @Test
    void eventTypesRoundTrip() {
        WebhookSubscription s = new WebhookSubscription();
        s.setEventTypes(Set.of(WebhookEventType.SUBMISSION_CREATED, WebhookEventType.SUBMISSION_APPROVED));
        Set<WebhookEventType> back = s.getEventTypes();
        assertEquals(2, back.size());
        assertTrue(back.contains(WebhookEventType.SUBMISSION_CREATED));
        assertTrue(back.contains(WebhookEventType.SUBMISSION_APPROVED));
    }

    @Test
    void emptyTypesProducesEmptySet() {
        WebhookSubscription s = new WebhookSubscription();
        s.setEventTypes(new LinkedHashSet<>());
        assertTrue(s.getEventTypes().isEmpty());
    }

    @Test
    void handlesRespectsActiveFlag() {
        WebhookSubscription s = new WebhookSubscription();
        s.setEventTypes(Set.of(WebhookEventType.SUBMISSION_CREATED));
        s.setActive(false);
        assertFalse(s.handles(WebhookEventType.SUBMISSION_CREATED));
        s.setActive(true);
        assertTrue(s.handles(WebhookEventType.SUBMISSION_CREATED));
        assertFalse(s.handles(WebhookEventType.SUBMISSION_REJECTED));
    }
}
