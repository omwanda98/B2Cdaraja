package com.B2Cdaraja.service;

import com.B2Cdaraja.model.GwRequest;
import com.B2Cdaraja.model.Result;
import com.B2Cdaraja.repository.PaymentRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;


import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DarajaServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, GwRequest> kafkaTemplate;

    @InjectMocks
    private DarajaService darajaService;

    private Validator validator;

    @BeforeEach
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        darajaService.setValidator(validator);
    }

    @Test
    public void testProcessB2CRequestSuccess() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        // Mock the API URL
        darajaService.setApiUrl(mockWebServer.url("/b2c").toString());
        darajaService.setTokenUrl(mockWebServer.url("/token").toString());

        // Mock the OAuth token response
        mockWebServer.enqueue(new MockResponse().setBody("{\"access_token\": \"mocked_token\"}"));

        // Mock the B2C API response
        mockWebServer.enqueue(new MockResponse().setBody("{\"ResponseCode\": \"0\", \"ResponseDescription\": \"Success\"}"));

        GwRequest gwRequest = new GwRequest();
        gwRequest.setId("1");
        gwRequest.setAmount(100);
        gwRequest.setMobileNumber("254700000000");
        gwRequest.setTransactionId("TRAN001");

        Result result = darajaService.processB2CRequest(gwRequest);

        assertEquals("Pending", result.getStatus());
        assertEquals("Service request successful", result.getResponseDescription());
        assertNotNull(result.getRef());
        verify(paymentRepository, times(1)).save(any(GwRequest.class));
        verify(kafkaTemplate, times(1)).send(anyString(), any(GwRequest.class));

        mockWebServer.shutdown();
    }

    @Test
    public void testProcessB2CRequestValidationFailure() {
        GwRequest gwRequest = new GwRequest();
        gwRequest.setId("1");
        gwRequest.setAmount(100);
        gwRequest.setMobileNumber("700000000"); // Invalid mobile number

        assertThrows(ConstraintViolationException.class, () -> {
            darajaService.processB2CRequest(gwRequest);
        });
    }

    @Test
    public void testFetchPaymentStatus() {
        GwRequest gwRequest = new GwRequest();
        gwRequest.setId("TRAN001");
        gwRequest.setStatus("Pending");
        gwRequest.setRef("ABCD123");

        when(paymentRepository.findById("TRAN001")).thenReturn(Optional.of(gwRequest));

        Result result = darajaService.fetchPaymentStatus("TRAN001");

        assertEquals("Pending", result.getStatus());
        assertEquals("ABCD123", result.getRef());
        assertEquals("Waiting for approval", result.getResponseDescription());
    }

    @Test
    public void testFetchPaymentStatusNotFound() {
        lenient().when(paymentRepository.findById("TRAN001")).thenReturn(Optional.empty());

        Result result = darajaService.fetchPaymentStatus("TRAN001");

        assertEquals("Not Found", result.getStatus());
        assertEquals("N/A", result.getRef());
        assertEquals("Request not found", result.getResponseDescription());
    }

    @Test
    public void testUpdatePaymentStatus() {
        GwRequest gwRequest = new GwRequest();
        gwRequest.setTransactionId("TRAN001");
        gwRequest.setStatus("Pending");
        gwRequest.setRef("ABCD123");

        Result resultUpdate = new Result();
        resultUpdate.setTransactionId("TRAN001");
        resultUpdate.setStatus("Completed");
        resultUpdate.setRef("ABCD123");

        when(paymentRepository.findById("TRAN001")).thenReturn(Optional.of(gwRequest));

        Result result = darajaService.updatePaymentStatus(resultUpdate);

        assertEquals("Completed", result.getStatus());
        verify(paymentRepository, times(1)).save(any(GwRequest.class));
    }
}
