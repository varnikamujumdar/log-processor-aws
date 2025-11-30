package com.logprocessor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.logprocessor.model.LogData;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IngestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SqsClient sqsClient;
    private final Gson gson;
    private final String queueUrl;

    public IngestHandler() {
        this.sqsClient = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
        this.gson = new Gson();
        this.queueUrl = System.getenv("QUEUE_URL"); // We'll set this as environment variable
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            // Get content type
            Map<String, String> headers = input.getHeaders();
            String contentType = headers.getOrDefault("Content-Type", "").toLowerCase();
            
            LogData logData = new LogData();
            
            // Process based on content type
            if (contentType.contains("application/json")) {
                // JSON payload
                Map<String, String> body = gson.fromJson(input.getBody(), Map.class);
                logData.setTenantId(body.get("tenant_id"));
                logData.setLogId(body.getOrDefault("log_id", UUID.randomUUID().toString()));
                logData.setText(body.get("text"));
                logData.setSource("json");
                
            } else if (contentType.contains("text/plain")) {
                // Plain text payload
                String tenantId = headers.get("X-Tenant-ID");
                if (tenantId == null || tenantId.isEmpty()) {
                    return createErrorResponse(400, "Missing X-Tenant-ID header");
                }
                logData.setTenantId(tenantId);
                logData.setLogId(UUID.randomUUID().toString());
                logData.setText(input.getBody());
                logData.setSource("text_upload");
                
            } else {
                return createErrorResponse(400, "Unsupported Content-Type. Use application/json or text/plain");
            }
            
            // Validate
            if (logData.getTenantId() == null || logData.getText() == null) {
                return createErrorResponse(400, "Missing required fields: tenant_id and text");
            }
            
            // Send to SQS
            String messageBody = gson.toJson(logData);
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            
            sqsClient.sendMessage(sendMsgRequest);
            
            // Return 202 Accepted
            response.setStatusCode(202);
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Accepted");
            responseBody.put("log_id", logData.getLogId());
            response.setBody(gson.toJson(responseBody));
            
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
        
        return response;
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        response.setBody(new Gson().toJson(body));
        return response;
    }
}