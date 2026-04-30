package com.microform.webhook.api;

import com.microform.webhook.domain.DeliveryStatus;
import com.microform.webhook.domain.WebhookDelivery;
import com.microform.webhook.domain.WebhookEventType;
import com.microform.webhook.domain.WebhookSubscription;
import com.microform.webhook.persistence.WebhookDeliveryRepository;
import com.microform.webhook.persistence.WebhookSubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookSubscriptionController {

    private final WebhookSubscriptionRepository subs;
    private final WebhookDeliveryRepository deliveries;

    public WebhookSubscriptionController(WebhookSubscriptionRepository subs,
                                         WebhookDeliveryRepository deliveries) {
        this.subs = subs;
        this.deliveries = deliveries;
    }

    @PostMapping
    public ResponseEntity<WebhookSubscription> create(@Valid @RequestBody Request req) {
        WebhookSubscription s = new WebhookSubscription();
        s.setName(req.name());
        s.setUrl(req.url());
        s.setSecret(req.secret());
        s.setEventTypes(req.eventTypes());
        s.setActive(req.active() == null ? true : req.active());
        subs.save(s);
        return ResponseEntity.created(URI.create("/webhooks/" + s.getId())).body(s);
    }

    @GetMapping
    public List<WebhookSubscription> list() {
        return subs.findAll();
    }

    @GetMapping("/{id}")
    public WebhookSubscription get(@PathVariable UUID id) {
        return subs.findById(id).orElseThrow(() -> new NoSuchElementException("subscription not found: " + id));
    }

    @PutMapping("/{id}")
    public WebhookSubscription update(@PathVariable UUID id, @Valid @RequestBody Request req) {
        var s = subs.findById(id).orElseThrow(() -> new NoSuchElementException("subscription not found: " + id));
        s.setName(req.name());
        s.setUrl(req.url());
        s.setSecret(req.secret());
        s.setEventTypes(req.eventTypes());
        s.setActive(req.active() == null ? s.isActive() : req.active());
        subs.save(s);
        return s;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subs.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    public List<WebhookDelivery> deliveries(@PathVariable UUID id) {
        return deliveries.findBySubscription(id);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "pending", deliveries.countByStatus(DeliveryStatus.PENDING),
                "succeeded", deliveries.countByStatus(DeliveryStatus.SUCCEEDED),
                "failed", deliveries.countByStatus(DeliveryStatus.FAILED),
                "deadLetter", deliveries.countByStatus(DeliveryStatus.DEAD_LETTER)
        );
    }

    public record Request(
            @NotBlank String name,
            @NotBlank String url,
            String secret,
            @NotNull @NotEmpty Set<WebhookEventType> eventTypes,
            Boolean active) {
    }
}
