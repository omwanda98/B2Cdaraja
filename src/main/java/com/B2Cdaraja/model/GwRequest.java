package com.B2Cdaraja.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


@Document(collection = "daraja_db")
public class GwRequest {

    @Id
    private String id; // For MongoDB internal use
    private String transactionId; // Unique identifier for the transaction
    
//    @NotNull(message = "CommandId cannot be null")
    private String commandId; // Command ID
    
//    @Min(value = 10, message = "Amount must be at least 10")
//    @Max(value = 150000, message = "Amount must not exceed 150,000")
    private double amount;
    
    //@Pattern(regexp = "^2547[0-9]{8}$", message = "Invalid mobile number format")
    private String mobileNumber;
    private String status;
    private String ref;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
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
}
