package com.B2Cdaraja.model;

public class Result {

    private String id;
    private String status;
    private String ref;
    private String responseDescription; 

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getResponseDescription() { // New getter
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) { // New setter
        this.responseDescription = responseDescription;
    }
}
