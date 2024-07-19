package com.B2Cdaraja.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.B2Cdaraja.model.GwRequest;
import com.B2Cdaraja.model.Result;
import com.B2Cdaraja.service.DarajaService;


@RestController
@RequestMapping("/api/v1/daraja")
public class DarajaController {

	//DI
    @Autowired
    private DarajaService darajaService;

    @PostMapping("/b2c")
    public Result sendB2CRequest(@RequestBody GwRequest gwRequest) {
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
}
