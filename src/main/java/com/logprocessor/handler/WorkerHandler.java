package com.logprocessor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.logprocessor.model.LogData;
import com.logprocessor.model.ProcessedLog;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

public class WorkerHandler implements RequestHandler<SQSEvent, Void> {

    private final DynamoDbClient dynamoDbClient;
    private final Gson gson;
    private final String tableName;

    public WorkerHandler() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .build();
        this.gson = new Gson();
        this.tableName = System.getenv("TABLE_NAME"); // We'll set this as environment variable
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                // Parse message
                String messageBody = message.getBody();
                LogData logData = gson.fromJson(messageBody, LogData.class);
                
                context.getLogger().log("Processing log for tenant: " + logData.getTenantId());
                
                // Simulate heavy processing (0.05s per character)
                String text = logData.getText();
                int sleepTimeMs = text.length() * 50; // 0.05s = 50ms per character
                
                context.getLogger().log("Simulating processing for " + sleepTimeMs + "ms");
                Thread.sleep(sleepTimeMs);
                
                // Process the text (redact phone numbers as example)
                String modifiedText = text.replaceAll("\\d{3}-\\d{4}", "[REDACTED]");
                
                // Create processed log
                ProcessedLog processedLog = new ProcessedLog(
                    logData.getTenantId(),
                    logData.getLogId(),
                    logData.getSource(),
                    text,
                    modifiedText
                );
                
                // Save to DynamoDB with tenant isolation
                saveToDynamoDB(processedLog);
                
                context.getLogger().log("Successfully processed log: " + logData.getLogId());
                
            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                throw new RuntimeException(e); // This will cause the message to retry
            }
        }
        return null;
    }
    
    private void saveToDynamoDB(ProcessedLog log) {
        // Create composite key for tenant isolation: tenants/{tenant_id}/processed_logs/{log_id}
        String pk = "TENANT#" + log.getTenantId();
        String sk = "LOG#" + log.getLogId();
        
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(pk).build());
        item.put("SK", AttributeValue.builder().s(sk).build());
        item.put("tenant_id", AttributeValue.builder().s(log.getTenantId()).build());
        item.put("log_id", AttributeValue.builder().s(log.getLogId()).build());
        item.put("source", AttributeValue.builder().s(log.getSource()).build());
        item.put("original_text", AttributeValue.builder().s(log.getOriginalText()).build());
        item.put("modified_data", AttributeValue.builder().s(log.getModifiedData()).build());
        item.put("processed_at", AttributeValue.builder().s(log.getProcessedAt()).build());
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        
        dynamoDbClient.putItem(request);
    }
}