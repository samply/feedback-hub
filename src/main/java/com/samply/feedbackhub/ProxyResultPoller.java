package com.samply.feedbackhub;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

public class ProxyResultPoller extends Thread {
    private final UUID taskUUID;
    private final int waitCount;
    private final ProxyResultListener resultListener;
    public ProxyResultPoller(UUID taskUUID, int waitCount, ProxyResultListener resultListener) {
        this.taskUUID = taskUUID;
        this.waitCount = waitCount;
        this.resultListener = resultListener;
    }

    public void run() {
        final String request_uri = System.getenv("BEAM_PROXY_URI") + "/v1/tasks/" + taskUUID + "/results?wait_count=" + waitCount;
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey app1.proxy1.broker App1Secret");
        HttpEntity<String> request = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(request_uri, HttpMethod.GET, request, String.class);

        System.out.println(response);

        if (response.getStatusCode() == HttpStatus.OK) {
            resultListener.onResult(HttpStatus.OK);
        } else {
            resultListener.onResult(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

