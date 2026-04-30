package com.microform.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microform.webhook.domain.DeliveryStatus;
import com.microform.webhook.domain.WebhookDelivery;
import com.microform.webhook.domain.WebhookEventType;
import com.microform.webhook.domain.WebhookSubscription;
import com.microform.webhook.persistence.WebhookDeliveryRepository;
import com.microform.webhook.persistence.WebhookSubscriptionRepository;
import com.microform.webhook.service.DefaultWebhookHttpClient;
import com.microform.webhook.service.WebhookDispatcher;
import com.microform.webhook.service.WebhookEventPublisher;
import com.microform.webhook.service.WebhookHttpClient;
import com.microform.webhook.service.WebhookSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "microform.webhook.scheduler-enabled=false",
        "microform.webhook.max-attempts=3",
        "microform.webhook.retry-base-ms=10",
        "microform.webhook.retry-max-ms=50"
})
class WebhookDispatcherTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        WebhookHttpClient testClient() {
            return new TestWebhookClient();
        }
    }

    static class TestWebhookClient implements WebhookHttpClient {
        final List<String> received = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        int responseStatus = 200;
        boolean throwOnce = false;
        boolean alwaysFail = false;

        @Override
        public int post(String url, String payloadJson, String signatureHeader) {
            calls.incrementAndGet();
            received.add(payloadJson);
            if (throwOnce) {
                throwOnce = false;
                throw new RuntimeException("boom");
            }
            if (alwaysFail) {
                throw new RuntimeException("boom-always");
            }
            return responseStatus;
        }
    }

    @Autowired WebhookSubscriptionRepository subs;
    @Autowired WebhookDeliveryRepository deliveries;
    @Autowired WebhookEventPublisher publisher;
    @Autowired WebhookDispatcher dispatcher;
    @Autowired WebhookHttpClient http;
    @Autowired ObjectMapper mapper;
    @Autowired DefaultWebhookHttpClient.WebhookProperties props;
    @Autowired WebhookSignatureService sig;
    @Autowired JdbcTemplate jdbc;

    TestWebhookClient client() { return (TestWebhookClient) http; }

    @BeforeEach
    void resetClient() {
        jdbc.update("DELETE FROM webhook_deliveries");
        jdbc.update("DELETE FROM webhook_subscriptions");
        TestWebhookClient c = client();
        c.received.clear();
        c.calls.set(0);
        c.responseStatus = 200;
        c.throwOnce = false;
        c.alwaysFail = false;
    }

    private WebhookSubscription createSub(WebhookEventType... types) {
        WebhookSubscription s = new WebhookSubscription();
        s.setName("sub-" + UUID.randomUUID());
        s.setUrl("http://example.test/hook");
        s.setSecret("secret");
        s.setEventTypes(Set.of(types));
        s.setActive(true);
        return subs.save(s);
    }

    @Test
    void publishEnqueuesPendingDelivery() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        var found = deliveries.findBySubscription(s.getId());
        assertEquals(1, found.size());
        assertEquals(DeliveryStatus.PENDING, found.get(0).getStatus());
    }

    @Test
    void inactiveSubscriptionsSkipped() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        s.setActive(false);
        subs.save(s);
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        assertTrue(deliveries.findBySubscription(s.getId()).isEmpty());
    }

    @Test
    void mismatchedEventTypeNotEnqueued() {
        var s = createSub(WebhookEventType.SUBMISSION_APPROVED);
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        assertTrue(deliveries.findBySubscription(s.getId()).isEmpty());
    }

    @Test
    void successfulDispatchMarksSucceeded() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        int n = dispatcher.dispatchBatch();
        assertEquals(1, n);
        var d = deliveries.findBySubscription(s.getId()).get(0);
        assertEquals(DeliveryStatus.SUCCEEDED, d.getStatus());
        assertEquals(1, d.getAttempts());
        assertEquals(200, d.getResponseStatus());
    }

    @Test
    void retryOnTransportError() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        client().throwOnce = true;
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));

        // first dispatch fails
        dispatcher.dispatchBatch();
        var d1 = deliveries.findBySubscription(s.getId()).get(0);
        assertEquals(DeliveryStatus.PENDING, d1.getStatus());
        assertEquals(1, d1.getAttempts());
        assertNotNull(d1.getNextAttemptAt());

        // second dispatch succeeds (sleep past backoff)
        try { Thread.sleep(80); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        dispatcher.dispatchBatch();
        var d2 = deliveries.findBySubscription(s.getId()).get(0);
        assertEquals(DeliveryStatus.SUCCEEDED, d2.getStatus());
        assertEquals(2, d2.getAttempts());
    }

    @Test
    void deadLetterAfterMaxAttempts() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        client().alwaysFail = true;
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));

        for (int i = 0; i < 4; i++) {
            dispatcher.dispatchBatch();
            try { Thread.sleep(80); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        var d = deliveries.findBySubscription(s.getId()).get(0);
        assertEquals(DeliveryStatus.DEAD_LETTER, d.getStatus());
        assertEquals(props.getMaxAttempts(), d.getAttempts());
        assertNull(d.getNextAttemptAt());
    }

    @Test
    void non2xxResponseTriggersRetry() {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        client().responseStatus = 500;
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        dispatcher.dispatchBatch();
        var d = deliveries.findBySubscription(s.getId()).get(0);
        assertEquals(DeliveryStatus.PENDING, d.getStatus());
        assertEquals(1, d.getAttempts());
        assertEquals(500, d.getResponseStatus());
    }

    @Test
    void exponentialBackoffMonotonic() {
        long b1 = dispatcher.nextDelayMs(1);
        long b2 = dispatcher.nextDelayMs(2);
        long b3 = dispatcher.nextDelayMs(3);
        assertTrue(b2 >= b1);
        assertTrue(b3 >= b2);
        long capped = dispatcher.nextDelayMs(50);
        assertTrue(capped <= props.getRetryMaxMs());
    }

    @Test
    void payloadEnvelopeContainsEventTimestampData() throws Exception {
        var s = createSub(WebhookEventType.SUBMISSION_CREATED);
        publisher.publish(WebhookEventType.SUBMISSION_CREATED, Map.of("foo", "bar"));
        var d = deliveries.findBySubscription(s.getId()).get(0);
        var json = mapper.readTree(d.getPayloadJson());
        assertEquals("SUBMISSION_CREATED", json.get("event").asText());
        assertNotNull(json.get("timestamp"));
        assertEquals("bar", json.get("data").get("foo").asText());
    }

    @Test
    void signatureGeneratedWhenSecretPresent() {
        String s = sig.sign("secret", "payload");
        assertNotNull(s);
        assertTrue(s.startsWith("sha256="));
    }
}
