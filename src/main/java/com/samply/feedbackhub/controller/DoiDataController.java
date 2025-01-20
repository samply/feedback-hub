package com.samply.feedbackhub.controller;

import com.samply.feedbackhub.Util;

import com.macasaet.fernet.Key;
import com.samply.feedbackhub.BeamTask;
import com.samply.feedbackhub.BeamTaskHelper;
import com.samply.feedbackhub.ProxyResultPoller;
import com.samply.feedbackhub.exception.DoiDataAlreadyPresentException;
import com.samply.feedbackhub.model.DoiData;
import com.samply.feedbackhub.model.DoiDataDto;
import com.samply.feedbackhub.repository.DoiDataRepository;
import com.samply.feedbackhub.exception.DoiDataNotFoundException;
import com.samply.feedbackhub.service.DoiDataService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


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

    private static final int FEEDBACK_AGENTS_COUNT = Integer.parseInt(System.getenv("FEEDBACK_AGENTS_COUNT"));

    // Get all DoiData
    // only for testing
    @GetMapping("/doi-data")
    public List<DoiData> getAllDoiData() {
        System.out.println("getAllDoiData: entered");
        return doiDataRepository.findAll();
    }

    // Create a new DoiData
    @PostMapping("/doi-data")
    public ResponseEntity<?> createDoiData(@Valid @RequestBody DoiDataDto doiDataDto) throws DoiDataAlreadyPresentException {
        System.out.println("createDoiData: entered");
        // Check whether DoiData is already present in the repository
        if (doiDataRepository.findByRequest(doiDataDto.getRequestID()).size() > 0) {
            System.out.println("Data already present");
            throw new DoiDataAlreadyPresentException(doiDataDto.getRequestID());
        }
        // Generate the symmetric encryption key
        final Key key = Key.generateKey();

        System.out.println("createDoiData: Create data entity and add it to local database");
        // Create data entity and add it to local database
        String uuid = UUID.randomUUID().toString();
        DoiData doiData = new DoiData(doiDataDto.getRequestID(), doiDataDto.getPublicationReference(), key.serialise(), uuid);
        long doiDataID = doiDataRepository.save(doiData).getId();

        System.out.println("createDoiData: requestID: " + doiDataDto.getRequestID());
        System.out.println("createDoiData: publicationReference: " + doiDataDto.getPublicationReference());
        System.out.println("createDoiData: key: " + key.serialise());
        System.out.println("createDoiData: uuid: " + uuid);
        System.out.println("createDoiData: doiDataID: " + doiDataID);
        System.out.println("createDoiData: " + Util.jsonStringFromObject(doiData));

        try {
            // Create and send the BeamTask to the proxy server
            BeamTask task = BeamTaskHelper.createBeamTask(doiData);
            System.out.println("createDoiData: send the BeamTask to the proxy server, task ID: " + task.getId());
            ResponseEntity<JSONObject> responseEntity = BeamTaskHelper.sendBeamTask(task);

            if (responseEntity.getStatusCode().equals(HttpStatus.CREATED)) {
                System.out.println("createDoiData: pollForTaskResults, doiDataID: " + doiDataID);
                return pollForTaskResults(doiDataID, task);
            } else {
                System.out.println("createDoiData: Something went wrong when sending Beam task, status: " + responseEntity.getStatusCode());
                doiDataService.deleteDoiDataById(doiDataID);
                return responseEntity;
            }
        } catch (Exception e) {
            System.out.println("createDoiData: Exception: " + Util.traceFromException(e));
            doiDataService.deleteDoiDataById(doiDataID);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Creates a poller and awaits for results from all agent proxies
    private ResponseEntity<Object> pollForTaskResults(long doiDataID, BeamTask task) {
        System.out.println("pollForTaskResults: entered");
        CompletableFuture<HttpStatus> resultFuture = new CompletableFuture<>();
        System.out.println("pollForTaskResults: resultFuture declared");
        ProxyResultPoller poller = new ProxyResultPoller(task.getId(), FEEDBACK_AGENTS_COUNT, status -> resultFuture.complete(status));
        System.out.println("pollForTaskResults: poller declared");
        poller.start();
        System.out.println("pollForTaskResults: poller started");
        try {
            System.out.println("pollForTaskResults: block and wait for the result");
            HttpStatus statusCode = resultFuture.get(); // Block and wait for the result
            System.out.println("pollForTaskResults: result available");
            if (statusCode.equals(HttpStatus.OK)) {
                System.out.println("pollForTaskResults: deleting doiDataID: " + doiDataID);
                doiDataService.deleteDoiDataById(doiDataID);
                return new ResponseEntity<>(statusCode);
            } else {
                System.out.println("pollForTaskResults: polling error, status: " + statusCode);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("pollForTaskResults: polling exception: " + Util.traceFromException(e));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get Doi by request ID (testing only)
    @GetMapping("/doi-token/{req_id}")
    public String getDoiTokenByRequestID(@PathVariable(value = "req_id") String requestId) throws DoiDataNotFoundException {
        System.out.println("getDoiTokenByRequestID: requestId: " + requestId);
        List<DoiData> data = doiDataRepository.findByRequest(requestId);
        if (data.size() == 0) throw new DoiDataNotFoundException(requestId);
        return data.get(0).getPublicationReferenceToken();
    }

    // Get Doi by request ID
    @GetMapping("/reference-token/{access_code}")
    public String getDoiTokenByAccessCode(@PathVariable(value = "access_code") String accessCode) throws DoiDataNotFoundException {
        System.out.println("getDoiTokenByAccessCode: accessCode: " + accessCode);
        List<DoiData> data = doiDataRepository.findByAccessCode(accessCode);
        if (data.size() == 0) {
            System.out.println("getDoiTokenByAccessCode: data array is empty");
            throw new DoiDataNotFoundException(accessCode);
        }
        String token =  data.get(0).getPublicationReferenceToken();
        System.out.println("getDoiTokenByAccessCode: token: " + token);
        return token;
    }
}
