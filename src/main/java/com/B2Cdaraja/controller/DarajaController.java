package com.B2Cdaraja.controller;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.B2Cdaraja.model.GwRequest;
import com.B2Cdaraja.model.Result;
import com.B2Cdaraja.service.DarajaService;

@RestController
@RequestMapping("/api/v1/daraja")
public class DarajaController {

    @Autowired
    private DarajaService darajaService;

    @PostMapping("/b2c")
    public Result sendB2CRequest(@RequestBody GwRequest gwRequest) {
        gwRequest.setTransactionId(generateUniqueTransactionId()); // Set the transactionId before processing
        return darajaService.processB2CRequest(gwRequest);
    }

    @GetMapping("/status/{transactionId}")
    public Result fetchPaymentStatus(@PathVariable String transactionId) {
        return darajaService.fetchPaymentStatus(transactionId);
    }

    @PostMapping("/update-status")
    public Result updatePaymentStatus(@RequestBody Result result) {
        return darajaService.updatePaymentStatus(result);
    }

    private String generateUniqueTransactionId() {
        SecureRandom random = new SecureRandom();
        StringBuilder transactionId = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            transactionId.append((char) ('A' + random.nextInt(26)));
        }
        for (int i = 0; i < 4; i++) {
            transactionId.append(random.nextInt(10));
        }
        return transactionId.toString();
    }
}
