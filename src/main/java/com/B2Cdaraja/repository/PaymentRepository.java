package com.B2Cdaraja.repository;

import com.B2Cdaraja.model.GwRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<GwRequest, String> {
    Optional<GwRequest> findByTransactionId(String transactionId);
}
