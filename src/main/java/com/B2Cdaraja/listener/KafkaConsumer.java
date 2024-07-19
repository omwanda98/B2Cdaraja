package com.B2Cdaraja.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.B2Cdaraja.model.Result;
import com.B2Cdaraja.repository.PaymentRepository;

@Service
public class KafkaConsumer {

    @Autowired
    private PaymentRepository paymentRepository;

    @KafkaListener(topics = "b2c-responses", groupId = "group_id")
    public void consume(Result result) {
        // Update MongoDB with the response status
        paymentRepository.findById(result.getId()).ifPresent(gwRequest -> {
            // Assuming a separate result repository exists to store the Result status and reference
            // Update the Result collection with status and reference
        });
    }
}
