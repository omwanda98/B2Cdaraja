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

import jakarta.validation.Validator;

import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

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
    
    @Autowired
    private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern KENYAN_PHONE_NUMBER_PATTERN = Pattern.compile("^2547[0-9]{8}$");
    private static final double MIN_AMOUNT = 10.0;
    private static final double MAX_AMOUNT = 150000.0;
    
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

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

    private boolean isValidKenyanSafaricomNumber(String mobileNumber) {
        return KENYAN_PHONE_NUMBER_PATTERN.matcher(mobileNumber).matches();
    }

    private boolean isValidAmount(double amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }
    
    private String generateUniqueRef() {
        SecureRandom random = new SecureRandom();
        StringBuilder ref = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            ref.append((char) ('A' + random.nextInt(26)));
        }
        for (int i = 0; i < 4; i++) {
            ref.append(random.nextInt(10));
        }
        return ref.toString();
    }


	public Result processB2CRequest(GwRequest gwRequest) {
        
        Result result = new Result();
        result.setTransactionId(gwRequest.getTransactionId());
        result.setCommandId(gwRequest.getCommandId());
        result.setStatus("Pending");
        result.setResponseDescription("Service request successful");

        if (!isValidKenyanSafaricomNumber(gwRequest.getMobileNumber())) {
            result.setStatus("Error");
            result.setResponseDescription("Invalid mobile number. Must be a valid Kenyan Safaricom number.");
            return result;
        }

        if (!isValidAmount(gwRequest.getAmount())) {
            result.setStatus("Error");
            result.setResponseDescription("Invalid amount. Must be between KSh 10 and KSh 150,000.");
            return result;
        }

        String uniqueRef = generateUniqueRef();
        result.setRef(uniqueRef);
        gwRequest.setRef(uniqueRef); // Set ref in GwRequest object

        try {
            String accessToken = getOAuthToken();

            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("Initiator", "testapi");
            jsonBody.put("SecurityCredential", "N3VlFrUDJGsJ+eqAXgm8Kr6RdRKQRCyrAPuheFiPvG/iFlYMyifCiHwxN9ehnl2t7Mw/zWWdMjFLbvCw+lOwQmrwFDeNY59Hw3JTBKfGkNdZN01RzlZAga7ZAn6070wnHe8VHGKWkefaMybGlBc3kdKLqXAK4Ri6MQLPN9bugHpyz/Gh7+fL3QIB5mSgEs85puFAZTyWM2HATOOMWnsEBMwlgEl8enBsp5VT82TWfIjqPjpsBob1KMBe83vS5ijuNaQS0WxuP1eG+clZv/lO/N+IGo6iqm79sUBN38T7bv6NJLU8Eh7oAe9OJSImbWgySFWRppo3Ejv6ij+QLVpanw==");
            jsonBody.put("CommandID", gwRequest.getCommandId());
            jsonBody.put("Amount", gwRequest.getAmount());
            jsonBody.put("PartyA", "600990");
            jsonBody.put("PartyB", gwRequest.getMobileNumber());
            jsonBody.put("Remarks", "Test remarks");
            jsonBody.put("QueueTimeOutURL", "https://mydomain.com/b2c/queue");
            jsonBody.put("ResultURL", "https://mydomain.com/b2c/result");
            jsonBody.put("occasion", "null");

            RequestBody body = RequestBody.create(mediaType, jsonBody.toString());

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                // Log the raw response body
                System.out.println("External API Response Body: " + responseBody);

                JSONObject responseJson = new JSONObject(responseBody);

                if (responseJson.has("ResponseCode")) {
                    result.setStatus(responseJson.getString("ResponseCode"));
                }

                if (responseJson.has("ResponseDescription")) {
                    result.setResponseDescription(responseJson.getString("ResponseDescription"));
                }

                // Save to MongoDB
                gwRequest.setStatus(result.getStatus()); // Ensure GwRequest's status is updated
                paymentRepository.save(gwRequest); // Save GwRequest with ref to MongoDB

                // Send to Kafka
                kafkaTemplate.send("b2c-requests", gwRequest);

            } catch (IOException e) {
                result.setStatus("Error");
                result.setRef("N/A");
                result.setResponseDescription("Error processing request");
                e.printStackTrace();
            }
        } catch (IOException e) {
            result.setStatus("Error");
            result.setRef("N/A");
            result.setResponseDescription("Error obtaining access token");
            e.printStackTrace();
        }

        return result;
    }

    //FETCHING THE PAYMENT STATUS
    public Result fetchPaymentStatus(String transactionId) {
        Optional<GwRequest> optionalGwRequest = paymentRepository.findByTransactionId(transactionId);
        Result result = new Result();
        result.setTransactionId(transactionId);

        if (optionalGwRequest.isPresent()) {
            GwRequest gwRequest = optionalGwRequest.get();
            result.setCommandId(gwRequest.getCommandId());
            result.setRef(gwRequest.getRef());
            result.setStatus(gwRequest.getStatus());

            // Set appropriate response description based on status
            if ("Pending".equals(gwRequest.getStatus())) {
                result.setResponseDescription("Waiting for approval");
            } else if ("Completed".equals(gwRequest.getStatus())) {
                result.setResponseDescription("Payment successful");
            } else if ("Error".equals(gwRequest.getStatus())) {
                result.setResponseDescription("Payment error or failure");
            } else {
                result.setResponseDescription("Unknown status");
            }
        } else {
            result.setStatus("Not Found");
            result.setRef("N/A");
            result.setResponseDescription("Request not found");
        }

        return result;
    }

    //UPDATE STATUS
    public Result updatePaymentStatus(Result result) {
        Optional<GwRequest> optionalGwRequest = paymentRepository.findByTransactionId(result.getTransactionId());
        if (optionalGwRequest.isPresent()) {
            GwRequest gwRequest = optionalGwRequest.get();

            // Debugging before update
            System.out.println("Before Update - TransactionId: " + gwRequest.getTransactionId() + ", Ref: " + gwRequest.getRef());

            // Update status and ref
            gwRequest.setStatus(result.getStatus());
            paymentRepository.save(gwRequest);

            // Debugging after update
            System.out.println("After Update - TransactionId: " + gwRequest.getTransactionId() + ", Ref: " + gwRequest.getRef());

            result.setResponseDescription("Payment status updated successfully");
            result.setCommandId(gwRequest.getCommandId());
            result.setRef(gwRequest.getRef());
        } else {
            result.setStatus("Not Found");
            result.setRef("N/A");
            result.setResponseDescription("Request not found");
        }

        return result;
    }
}