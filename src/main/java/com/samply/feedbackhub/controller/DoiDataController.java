package com.samply.feedbackhub.controller;
import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import com.samply.feedbackhub.BeamTask;
import com.samply.feedbackhub.exception.DoiDataAlreadyPresentException;
import com.samply.feedbackhub.model.DoiData;
import com.samply.feedbackhub.repository.DoiDataRepository;
import com.samply.feedbackhub.exception.DoiDataNotFoundException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.client.RestTemplate;


import java.util.LinkedList;
import java.util.List;

@RestController
public class DoiDataController {
    @Autowired
    DoiDataRepository doiDataRepository;

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
    public DoiData createDoiData(@Valid @RequestBody DoiData doi_data) throws DoiDataAlreadyPresentException {
        if (doiDataRepository.findByRequest(doi_data.getRequestID()).size() > 0) {
            throw new DoiDataAlreadyPresentException(doi_data.getRequestID());
        }
        final Key key = Key.generateKey();
        doi_data.setSymEncKey(key.serialise());
        DoiData returnData = doiDataRepository.save(doi_data);

        BeamTask task = new BeamTask();
        task.setFrom("app1.proxy1.broker");

        LinkedList<String> toList = new LinkedList<>();
        toList.add("app1.proxy2.broker");
        task.setTo(toList);
        task.setBody(doi_data.getSymEncKey());
        task.setBackoffMillisecs(1000);
        task.setMaxTries(5);
        task.setTtl("30s");
        task.setMetadata(doi_data.getRequestID());

        final String uri = "http://dev_proxy1_1:8081/v1/tasks";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey app1.proxy1.broker App1Secret");

        HttpEntity<JSONObject> request = new HttpEntity<>(task.buildJSON(), headers);
        System.out.println(request);
        JSONObject result = restTemplate.postForObject(uri, request, JSONObject.class);
        System.out.println(result);

        returnData.setSymEncKey("hidden");
        return returnData;
    }

    // Get a Single DoiData
    /*@CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback/{id}")
    public DoiData getDoiDataById(@PathVariable(value = "id") Long specimenFeedbackId) throws DoiDataNotFoundException {
        return doiDataRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new DoiDataNotFoundException(specimenFeedbackId));
    }*/

    // Get SpecimenFeedbacks by request ID
    @CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/doi-token/{req_id}")
    public String getDoiTokenByRequestID(@PathVariable(value = "req_id") String requestId) throws DoiDataNotFoundException {
        List<DoiData> data = doiDataRepository.findByRequest(requestId);
        if (data.size() == 0) throw new DoiDataNotFoundException(requestId);
        return Token.generate(new Key(data.get(0).getSymEncKey()), data.get(0).getPublicationReference()).serialise();
    }
}