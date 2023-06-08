package com.samply.feedbackhub.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macasaet.fernet.Key;
import com.samply.feedbackhub.BeamTask;
import com.samply.feedbackhub.ProxyResultListener;
import com.samply.feedbackhub.ProxyResultPoller;
import com.samply.feedbackhub.exception.DoiDataAlreadyPresentException;
import com.samply.feedbackhub.model.DoiData;
import com.samply.feedbackhub.model.DoiDataDto;
import com.samply.feedbackhub.repository.DoiDataRepository;
import com.samply.feedbackhub.exception.DoiDataNotFoundException;
import com.samply.feedbackhub.service.DoiDataService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.client.RestTemplate;


import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class DoiDataController {
    @Autowired
    DoiDataRepository doiDataRepository;

    @Autowired
    DoiDataService doiDataService;

    // Get all DoiData
    // only for testing
    @CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/doi-data")
    public List<DoiData> getAllDoiData() {
        return doiDataRepository.findAll();
    }

    // Create a new DoiData
    @CrossOrigin(origins = "http://localhost:9000")
    @PostMapping("/doi-data")
    public ResponseEntity<?> createDoiData(@Valid @RequestBody DoiDataDto doiDataDto) throws DoiDataAlreadyPresentException {
        // Check if the DoiData is already present in the repository
        if (doiDataRepository.findByRequest(doiDataDto.getRequestID()).size() > 0) {
            throw new DoiDataAlreadyPresentException(doiDataDto.getRequestID());
        }
        // Generate the symmetric encryption key
        final Key key = Key.generateKey();

        // Create data entity and add it to local database
        DoiData doiData = new DoiData(doiDataDto.getRequestID(), doiDataDto.getPublicationReference(), key.serialise(), UUID.randomUUID().toString());

        // Create and send the BeamTask to the proxy server
        BeamTask task = createBeamTask(doiData);
        ResponseEntity<JSONObject> responseEntity = sendBeamTask(task);

        if (responseEntity.getStatusCodeValue() == 201) {
            // Add data entity to local database
            DoiData savedDoiData = doiDataRepository.save(doiData);
            long doiDataID = savedDoiData.getId();

            CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
            ProxyResultPoller poller = new ProxyResultPoller(task.getId(), Integer.parseInt(System.getenv("FEEDBACK_AGENTS_COUNT")), new ProxyResultListener() {
                @Override
                public void onResult(HttpStatus status) {
                    resultFuture.complete(status.value());
                }
            });
            poller.start();
            try {
                Integer statusCode = resultFuture.get(); // Block and wait for the result
                if (statusCode.equals(HttpStatus.OK.value())) {
                    doiDataService.deleteDoiDataById(doiDataID);
                    return new ResponseEntity<>(statusCode, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return responseEntity;
        }
    }

    private BeamTask createBeamTask(DoiData dataWithDoi) {
        BeamTask task = new BeamTask();
        task.setId(UUID.fromString(dataWithDoi.getAccessCode()));
        task.setFrom(System.getenv("FEEDBACK_HUB_BEAM_ID"));

        LinkedList<String> toList = new LinkedList<>();
        String agentBeamIds = System.getenv("FEEDBACK_AGENT_BEAM_IDS");

        // Parse the array
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String[] arrayValues = objectMapper.readValue(agentBeamIds, String[].class);

            for (String value : arrayValues) {
                toList.add(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        task.setTo(toList);

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("key", dataWithDoi.getSymEncKey());
        bodyJson.put("requestId", dataWithDoi.getRequestID());
        bodyJson.put("accessCode", dataWithDoi.getAccessCode());
        task.setBody(bodyJson.toString());

        task.setBackoffMillisecs(Integer.parseInt(System.getenv("PROXY_TASK_BACKOFF_MS")));
        task.setMaxTries(Integer.parseInt(System.getenv("PROXY_TASK_MAX_TRIES")));
        task.setTtl(System.getenv("PROXY_TASK_TTL"));
        task.setMetadata(null);
        return task;
    }

    private ResponseEntity<JSONObject> sendBeamTask(BeamTask task) {
        final String request_uri = System.getenv("BEAM_PROXY_URI") + "/v1/tasks";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey " + System.getenv("FEEDBACK_HUB_BEAM_ID") + " App1Secret");
        HttpEntity<JSONObject> request = new HttpEntity<>(task.buildJSON(), headers);

        return restTemplate.postForEntity(request_uri, request, JSONObject.class);
    }
    // Get Doi by request ID
    @GetMapping("/doi-token/{req_id}")
    public String getDoiTokenByRequestID(@PathVariable(value = "req_id") String requestId) throws DoiDataNotFoundException {
        List<DoiData> data = doiDataRepository.findByRequest(requestId);
        if (data.size() == 0) throw new DoiDataNotFoundException(requestId);
        return data.get(0).getPublicationReferenceToken();
    }
    // Get Doi by request ID
    @GetMapping("/reference-token/{access_code}")
    public String getDoiTokenByAccessCode(@PathVariable(value = "access_code") String accessCode) throws DoiDataNotFoundException {
        List<DoiData> data = doiDataRepository.findByAccessCode(accessCode);
        if (data.size() == 0) throw new DoiDataNotFoundException(accessCode);
        return data.get(0).getPublicationReferenceToken();
    }
}