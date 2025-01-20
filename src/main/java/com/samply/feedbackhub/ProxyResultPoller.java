package com.samply.feedbackhub;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

public class ProxyResultPoller extends Thread {
    private final UUID taskUUID;
    private final int waitCount;
    private final ProxyResultListener resultListener;
    private static final String BEAM_PROXY_URI = System.getenv("BEAM_PROXY_URI");
    private static final String FEEDBACK_HUB_BEAM_ID = System.getenv("FEEDBACK_HUB_BEAM_ID");
    private static final String FEEDBACK_HUB_SECRET = System.getenv("FEEDBACK_HUB_SECRET");
    /**
     * Constructs a new {@code ProxyResultPoller} with the specified parameters.
     *
     * @param taskUUID       The unique identifier of the task to poll.
     * @param waitCount      The number of times to wait for the result.
     * @param resultListener The listener to be notified about the result.
     */
    public ProxyResultPoller(UUID taskUUID, int waitCount, ProxyResultListener resultListener) {
        this.taskUUID = taskUUID;
        this.waitCount = waitCount;
        this.resultListener = resultListener;
    }
    /**
     * Runs the thread, sending a GET request to the Beam proxy and notifying
     * the result listener based on the HTTP status code of the response.
     * It waits until all adressed agent proxies return a result
     */
    @Override
    public void run() {
        final String requestUri = BEAM_PROXY_URI + "/v1/tasks/" + taskUUID + "/results?wait_count=" + waitCount;
        //final String requestUri = BEAM_PROXY_URI + "/v1/tasks/" + "b1eb828e-4970-469a-80dd-3058a03b8a93" + "/results?wait_count=" + waitCount;
        System.out.println("run: requestUri: " + requestUri);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + FEEDBACK_HUB_BEAM_ID + " " + FEEDBACK_HUB_SECRET);
            System.out.println("run: authorization: " + "ApiKey " + FEEDBACK_HUB_BEAM_ID + " " + FEEDBACK_HUB_SECRET);
            HttpEntity<?> request = new HttpEntity<>(headers);

            System.out.println("run: send request to Beam");
            ResponseEntity<String> response = restTemplate.exchange(requestUri, HttpMethod.GET, request, String.class);
            System.out.println("run: got response from Beam");

            if (response.getStatusCode() == HttpStatus.OK) {
                resultListener.onResult(HttpStatus.OK);
            } else {
                System.out.println("run: Error: " + response.getStatusCode());
                resultListener.onResult(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            System.out.println("run: Exception: " + Util.traceFromException(e));
            resultListener.onResult(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        System.out.println("run: done");
    }
}
