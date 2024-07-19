package com.B2Cdaraja.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.B2Cdaraja.model.GwRequest;

//@Repository
public interface PaymentRepository extends MongoRepository<GwRequest, String> {
}