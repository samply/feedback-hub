package com.samply.feedbackhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samply.feedbackhub.model.DoiData;
import org.json.simple.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeamTaskHelper {
    private static final String BEAM_PROXY_URI = System.getenv("BEAM_PROXY_URI");
    private static final String FEEDBACK_HUB_BEAM_ID = System.getenv("FEEDBACK_HUB_BEAM_ID");
    private static final String FEEDBACK_HUB_SECRET = System.getenv("FEEDBACK_HUB_SECRET");
    private static final String FEEDBACK_AGENT_BEAM_IDS = System.getenv("FEEDBACK_AGENT_BEAM_IDS");
    private static final String PROXY_TASK_TTL = System.getenv("PROXY_TASK_TTL");
    private static final int PROXY_TASK_BACKOFF_MS = Integer.parseInt(System.getenv("PROXY_TASK_BACKOFF_MS"));
    private static final int PROXY_TASK_MAX_TRIES = Integer.parseInt(System.getenv("PROXY_TASK_MAX_TRIES"));

    public static BeamTask createBeamTask(DoiData dataWithDoi) {
        BeamTask task = new BeamTask();
        task.setId(UUID.fromString(dataWithDoi.getAccessCode()));
        task.setFrom(FEEDBACK_HUB_BEAM_ID);

        List<String> toList = parseAgentBeamIds(FEEDBACK_AGENT_BEAM_IDS);
        toList.add(FEEDBACK_HUB_BEAM_ID); // Add the hub so that we can list the task locally
        task.setTo(toList);

        JSONObject bodyJson = buildBeamTaskBody(dataWithDoi);
        task.setBody(bodyJson.toString());

        task.setBackoffMillisecs(PROXY_TASK_BACKOFF_MS);
        task.setMaxTries(PROXY_TASK_MAX_TRIES);
        task.setTtl(PROXY_TASK_TTL);
        task.setMetadata(null);

        return task;
    }

    private static List<String> parseAgentBeamIds(String agentBeamIds) {
        List<String> toList = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String[] arrayValues = objectMapper.readValue(agentBeamIds, String[].class);
            Collections.addAll(toList, arrayValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return toList;
    }

    private static JSONObject buildBeamTaskBody(DoiData dataWithDoi) {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("key", dataWithDoi.getSymEncKey());
        bodyJson.put("requestId", dataWithDoi.getRequestID());
        bodyJson.put("accessCode", dataWithDoi.getAccessCode());
        return bodyJson;
    }

    public static ResponseEntity<JSONObject> sendBeamTask(BeamTask task) {
        final String requestUri = BEAM_PROXY_URI + "/v1/tasks";
        System.out.println("sendBeamTask: task: " + Util.jsonStringFromObject(task));
        String authorizationHeader = "ApiKey " + FEEDBACK_HUB_BEAM_ID + " " + FEEDBACK_HUB_SECRET;
        System.out.println("sendBeamTask: authorizationHeader: " + authorizationHeader);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<JSONObject> request = new HttpEntity<>(task.buildJSON(), headers);

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForEntity(requestUri, request, JSONObject.class);
    }
}
