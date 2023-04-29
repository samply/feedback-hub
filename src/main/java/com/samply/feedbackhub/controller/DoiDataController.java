package com.samply.feedbackhub.controller;
import com.macasaet.fernet.Key;
import com.samply.feedbackhub.BeamTask;
import com.samply.feedbackhub.exception.DoiDataAlreadyPresentException;
import com.samply.feedbackhub.model.DoiData;
import com.samply.feedbackhub.model.DoiDataDto;
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
    public DoiData createDoiData(@Valid @RequestBody DoiDataDto doiDataDto) throws DoiDataAlreadyPresentException {
        // Check if the DoiData is already present in the repository
        if (doiDataRepository.findByRequest(doiDataDto.getRequestID()).size() > 0) {
            throw new DoiDataAlreadyPresentException(doiDataDto.getRequestID());
        }

        // Generate the symmetric encryption key
        final Key key = Key.generateKey();

        // Create data entity and add it to local database
        DoiData doiData = new DoiData(doiDataDto.getRequestID(), doiDataDto.getPublicationReference(), key.serialise());
        doiDataRepository.save(doiData);

        // Create and send the BeamTask to the proxy server
        BeamTask task = createBeamTask(doiData);
        sendBeamTask(task);

        return doiData;
    }

    private BeamTask createBeamTask(DoiData dataWithDoi) {
        BeamTask task = new BeamTask();
        task.setFrom("app1.proxy1.broker");

        LinkedList<String> toList = new LinkedList<>();
        toList.add("app1.proxy2.broker");
        task.setTo(toList);

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("key", dataWithDoi.getSymEncKey());
        bodyJson.put("requestId", dataWithDoi.getRequestID());
        bodyJson.put("dataToken", dataWithDoi.getPublicationReferenceToken());
        task.setBody(bodyJson.toString());

        task.setBackoffMillisecs(Integer.parseInt(System.getenv("PROXY_TASK_BACKOFF_MS")));
        task.setMaxTries(Integer.parseInt(System.getenv("PROXY_TASK_MAX_TRIES")));
        task.setTtl(System.getenv("PROXY_TASK_TTL"));
        task.setMetadata(null);
        return task;
    }

    private JSONObject sendBeamTask(BeamTask task) {
        final String request_uri = System.getenv("BEAM_PROXY_URI") + "/v1/tasks";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey app1.proxy1.broker App1Secret");
        HttpEntity<JSONObject> request = new HttpEntity<>(task.buildJSON(), headers);

        return restTemplate.postForObject(request_uri, request, JSONObject.class);
    }

    // Get a Single DoiData
    /*@CrossOrigin(origins = "http://localhost:9000")
    @GetMapping("/specimen-feedback/{id}")
    public DoiData getDoiDataById(@PathVariable(value = "id") Long specimenFeedbackId) throws DoiDataNotFoundException {
        return doiDataRepository.findById(specimenFeedbackId)
                .orElseThrow(() -> new DoiDataNotFoundException(specimenFeedbackId));
    }*/
    // Get symmetricaly encrypted Doi by request ID
    /*@GetMapping("/doi-token/{sym_key}")
    public String getDoiTokenBySymKey(@PathVariable(value = "sym_key") String requestId) throws DoiDataNotFoundException {
        List<DoiData> data = doiDataRepository.findByRequest(requestId);
        if (data.size() == 0) throw new DoiDataNotFoundException(requestId);
        return Token.generate(new Key(data.get(0).getSymEncKey()), data.get(0).getPublicationReference()).serialise();
    }*/
    // Get Doi by request ID
    @GetMapping("/doi-token/{req_id}")
    public String getDoiTokenByRequestID(@PathVariable(value = "req_id") String requestId) throws DoiDataNotFoundException {
        List<DoiData> data = doiDataRepository.findByRequest(requestId);
        if (data.size() == 0) throw new DoiDataNotFoundException(requestId);
        return data.get(0).getPublicationReferenceToken();
    }
}