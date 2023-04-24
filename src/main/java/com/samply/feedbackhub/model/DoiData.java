package com.samply.feedbackhub.model;

import jakarta.persistence.*;
@Entity
@Table(name = "doi_data")
public class DoiData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "request_id")
    private String requestID;

    @Column(name = "publication_reference")
    private String publicationReference;

    @Column(name = "sym_enc_key")
    private String symEncKey;
    public DoiData() {
        super();
    }

    public DoiData(long id, String requestID, String publicationReference, String symEncKey) {
        this.id = id;
        this.requestID = requestID;
        this.publicationReference = publicationReference;
        this.symEncKey = symEncKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getPublicationReference() {
        return publicationReference;
    }

    public void setPublicationReference(String publicationReference) {
        this.publicationReference = publicationReference;
    }

    public String getSymEncKey() {
        return symEncKey;
    }

    public void setSymEncKey(String encryptionToken) {
        this.symEncKey = encryptionToken;
    }
}