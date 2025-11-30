package com.logprocessor.model;

import java.time.Instant;

public class ProcessedLog {
    private String tenantId;
    private String logId;
    private String source;
    private String originalText;
    private String modifiedData;
    private String processedAt;

    // Constructors
    public ProcessedLog() {}

    public ProcessedLog(String tenantId, String logId, String source, 
                       String originalText, String modifiedData) {
        this.tenantId = tenantId;
        this.logId = logId;
        this.source = source;
        this.originalText = originalText;
        this.modifiedData = modifiedData;
        this.processedAt = Instant.now().toString();
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getModifiedData() {
        return modifiedData;
    }

    public void setModifiedData(String modifiedData) {
        this.modifiedData = modifiedData;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}