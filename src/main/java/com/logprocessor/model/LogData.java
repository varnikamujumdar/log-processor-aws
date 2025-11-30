package com.logprocessor.model;

public class LogData {

    private String tenantId;
    private String logId;
    private String text;
    private String source;

    public LogData()
    {
        
    }

    public LogData(String tenantId, String logId, String text, String source) {
        this.tenantId = tenantId;
        this.logId = logId;
        this.text = text;
        this.source = source;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
