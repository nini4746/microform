package com.microform.webhook;

import com.microform.webhook.service.WebhookSignatureService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureServiceTest {

    private final WebhookSignatureService svc = new WebhookSignatureService();

    @Test
    void nullOrBlankSecretReturnsNull() {
        assertNull(svc.sign(null, "{}"));
        assertNull(svc.sign("", "{}"));
        assertNull(svc.sign("   ", "{}"));
    }

    @Test
    void deterministicHmacSha256() {
        String s1 = svc.sign("topsecret", "{\"a\":1}");
        String s2 = svc.sign("topsecret", "{\"a\":1}");
        assertEquals(s1, s2);
        assertTrue(s1.startsWith("sha256="));
        assertEquals(7 + 64, s1.length()); // sha256= + 64 hex chars
    }

    @Test
    void differentPayloadsDifferentSignatures() {
        String s1 = svc.sign("topsecret", "{\"a\":1}");
        String s2 = svc.sign("topsecret", "{\"a\":2}");
        assertNotEquals(s1, s2);
    }
}
