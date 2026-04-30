package com.microform.webhook.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "microform.webhook", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookScheduler.class);

    private final WebhookDispatcher dispatcher;
    private final DefaultWebhookHttpClient.WebhookProperties props;
    private ScheduledExecutorService exec;

    public WebhookScheduler(WebhookDispatcher dispatcher, DefaultWebhookHttpClient.WebhookProperties props) {
        this.dispatcher = dispatcher;
        this.props = props;
    }

    @PostConstruct
    public void start() {
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webhook-dispatcher");
            t.setDaemon(true);
            return t;
        });
        long interval = props.getPollIntervalMs();
        exec.scheduleWithFixedDelay(this::tick, interval, interval, TimeUnit.MILLISECONDS);
        log.info("Webhook scheduler started; poll={}ms batch={}", interval, props.getBatchSize());
    }

    @PreDestroy
    public void stop() {
        if (exec != null) exec.shutdownNow();
    }

    private void tick() {
        try {
            dispatcher.dispatchBatch();
        } catch (Exception ex) {
            log.warn("Webhook dispatch tick failed: {}", ex.getMessage());
        }
    }
}
