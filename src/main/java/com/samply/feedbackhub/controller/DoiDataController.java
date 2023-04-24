package com.samply.feedbackhub.controller;
import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import com.samply.feedbackhub.exception.DoiDataAlreadyPresentException;
import com.samply.feedbackhub.model.DoiData;
import com.samply.feedbackhub.repository.DoiDataRepository;
import com.samply.feedbackhub.exception.DoiDataNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
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
        };
        final Key key = Key.generateKey();
        doi_data.setSymEncKey(key.serialise());

        DoiData returnData = doiDataRepository.save(doi_data); //shouldn't return key
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