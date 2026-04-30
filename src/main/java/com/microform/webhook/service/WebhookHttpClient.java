package com.microform.webhook.service;

public interface WebhookHttpClient {

    /**
     * Returns the response status code for the POST. Throws on transport error.
     */
    int post(String url, String payloadJson, String signatureHeader);
}
