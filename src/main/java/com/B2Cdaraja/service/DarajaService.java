package com.B2Cdaraja.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.B2Cdaraja.model.GwRequest;
import com.B2Cdaraja.model.Result;
import com.B2Cdaraja.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Service
public class DarajaService {

    @Value("${daraja.api.url}")
    private String apiUrl;

    @Value("${daraja.api.token.url}")
    private String tokenUrl;

    @Value("${daraja.api.consumer.key}")
    private String consumerKey;

    @Value("${daraja.api.consumer.secret}")
    private String consumerSecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, GwRequest> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //handle authentication 
    private String getOAuthToken() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url(tokenUrl)
                .method("GET", null)
                .addHeader("Authorization", "Basic " + encodedCredentials)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonObject = new JSONObject(responseBody);
                return jsonObject.getString("access_token");
            } else {
                throw new IOException("Failed to obtain access token");
            }
        }
    }

    //GwRequest
    public Result processB2CRequest(GwRequest gwRequest) {
        Result result = new Result();
        result.setId(gwRequest.getId());

        try {
            String accessToken = getOAuthToken();

            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");

            String jsonBody = objectMapper.writeValueAsString(gwRequest);
            RequestBody body = RequestBody.create(mediaType, jsonBody);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                // Log the response body for debugging
                System.out.println("Response Body: " + responseBody);

                JSONObject responseJson = new JSONObject(responseBody);

                // Set status and reference based on the actual response fields
                if (responseJson.has("ResponseCode")) {
                    result.setStatus(responseJson.getString("ResponseCode"));
                } else {
                    result.setStatus("Unknown");
                }

                if (responseJson.has("ResponseDescription")) {
                    result.setRef(responseJson.getString("ResponseDescription"));
                } else {
                    result.setRef("N/A");
                }

                // Save to MongoDB
                paymentRepository.save(gwRequest);

                // Send to Kafka
                kafkaTemplate.send("b2c-requests", gwRequest);

            } catch (IOException e) {
                result.setStatus("Error");
                result.setRef("N/A");
                e.printStackTrace();
            }
        } catch (IOException e) {
            result.setStatus("Error");
            result.setRef("N/A");
            e.printStackTrace();
        }

        return result;
    }

    public Result fetchPaymentStatus(String transactionId) {
        Optional<GwRequest> optionalGwRequest = paymentRepository.findById(transactionId);
        if (optionalGwRequest.isPresent()) {
            GwRequest gwRequest = optionalGwRequest.get();
            Result result = new Result();
            result.setId(gwRequest.getId());
            result.setStatus("Pending"); // Replace with actual status retrieval
            result.setRef("N/A"); // Replace with actual reference retrieval
            return result;
        } else {
            Result result = new Result();
            result.setId(transactionId);
            result.setStatus("Not Found");
            result.setRef("N/A");
            return result;
        }
    }

    public Result updatePaymentStatus(Result result) {
        Optional<GwRequest> optionalGwRequest = paymentRepository.findById(result.getId());
        if (optionalGwRequest.isPresent()) {
            GwRequest gwRequest = optionalGwRequest.get();
            // Update additional fields if necessary based on the Result object
            // Assuming a separate result repository exists to store the Result status and reference
            // Save the result to a separate collection if needed
        }
        return result;
    }
}
