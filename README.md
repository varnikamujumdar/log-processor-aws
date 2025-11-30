# AWS Serverless Log Processor

A multi-tenant, event-driven log processing pipeline built with AWS Lambda, SQS, and DynamoDB. Demonstrates serverless architecture, asynchronous processing, and strict data isolation at scale.

## 📖 Project Overview

This system processes log data from multiple tenants (customers) through a unified REST API, handling high-throughput ingestion while maintaining data isolation and resilience.

**Key Challenge Solved:** How to accept thousands of log entries per minute, process them asynchronously without blocking the API, and ensure each customer's data remains strictly separated.

## 🏗️ Architecture
```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌─────────┐     ┌──────────────────┐     ┌──────────┐
│  Client  │────▶│ API Gateway  │────▶│ IngestHandler   │────▶│   SQS   │────▶│ WorkerHandler    │────▶│ DynamoDB │
│          │     │  (REST API)  │     │    (Lambda)     │     │  Queue  │     │    (Lambda)      │     │          │
└──────────┘     └──────────────┘     └─────────────────┘     └─────────┘     └──────────────────┘     └──────────┘
                                              │                                         │
                                       Returns 202 Accepted                      Async Processing
                                       (~100ms response)                         (2-10s per message)
```

### Request Flow

1. **Client** sends HTTP POST with log data (JSON or plain text)
2. **API Gateway** routes request to IngestHandler Lambda
3. **IngestHandler** validates input, normalizes data, and publishes to SQS queue
4. **API returns 202 Accepted immediately** (non-blocking)
5. **SQS** acts as buffer, holding messages for processing
6. **WorkerHandler** Lambda automatically triggered by SQS messages
7. **WorkerHandler** simulates heavy processing (0.05s per character), then saves to DynamoDB
8. **DynamoDB** stores processed data with tenant isolation via partition keys

### Multi-Tenant Data Isolation

Each tenant's data is physically separated using DynamoDB composite keys:
```
Partition Key (PK): TENANT#<tenant_id>
Sort Key (SK):      LOG#<log_id>

Example:
├─ TENANT#acme
│  ├─ LOG#123
│  ├─ LOG#456
│  └─ LOG#789
│
└─ TENANT#beta_inc
   ├─ LOG#001
   ├─ LOG#002
   └─ LOG#003
```

**Benefits:**
- Physical data separation at the storage layer
- No possibility of cross-tenant data leakage
- Efficient DynamoDB queries scoped to single tenant
- Compliance with data isolation requirements

## ✨ Key Features

- **🚀 High Throughput**: Handles 1000+ requests per minute
- **⚡ Non-Blocking API**: Returns 202 Accepted in ~100ms, processing happens asynchronously
- **🔒 Multi-Tenant**: Strict data isolation per customer using DynamoDB partition keys
- **📈 Auto-Scaling**: Lambda functions scale automatically based on load (0 to 100+ instances)
- **🛡️ Crash Resilient**: SQS ensures zero message loss with automatic retries and visibility timeout
- **☁️ Serverless**: Zero infrastructure management, scales to zero when idle
- **💰 Cost-Efficient**: Pay only for execution time, ~$0 within AWS Free Tier

## 🛠️ Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Language | Java 17 | Lambda function implementation |
| Build Tool | Maven | Dependency management and packaging |
| API Layer | AWS API Gateway | REST API endpoint |
| Compute | AWS Lambda | Serverless function execution |
| Message Queue | AWS SQS | Async processing buffer |
| Database | AWS DynamoDB | NoSQL storage with tenant isolation |
| IAM | AWS IAM Roles | Least-privilege permissions |

## 📁 Project Structure
```
log-processor-aws/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── logprocessor/
│                   ├── handler/
│                   │   ├── IngestHandler.java       # API Lambda: validates and queues
│                   │   └── WorkerHandler.java       # Worker Lambda: processes and stores
│                   └── model/
│                       ├── LogData.java             # Internal data model
│                       └── ProcessedLog.java        # DynamoDB entity model
├── pom.xml                                          # Maven dependencies
├── trust-policy.json                                # IAM trust policy for Lambda
└── README.md
```

### Component Details

**IngestHandler.java**
- Accepts JSON and plain text payloads
- Validates required fields (tenant_id, text)
- Normalizes data into internal format
- Publishes to SQS queue
- Returns 202 Accepted immediately

**WorkerHandler.java**
- Triggered by SQS messages
- Simulates CPU-bound processing (0.05s per character)
- Processes data (e.g., redacts phone numbers)
- Saves to DynamoDB with tenant isolation
- Handles errors with automatic SQS retries

## 🚀 Getting Started

### Prerequisites

- AWS Account with Free Tier access
- AWS CLI installed and configured
- Java 17 or higher
- Maven 3.6 or higher
- Git

### Build the Project
```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/log-processor-aws.git
cd log-processor-aws

# Build with Maven
mvn clean package

# Verify JAR file created
ls -lh target/log-processor-1.0-SNAPSHOT.jar
# Should be ~13 MB (includes all dependencies)
```

### Deploy to AWS

The deployment involves creating these AWS resources:

1. **SQS Queue** - Message buffer (`log-processing-queue`)
2. **DynamoDB Table** - Storage with PK/SK structure (`ProcessedLogs`)
3. **IAM Role** - Lambda execution permissions
4. **Lambda Functions** - IngestHandler and WorkerHandler
5. **API Gateway** - REST API endpoint

**Note:** Detailed AWS CLI deployment commands are available upon request. The system can also be deployed through AWS Console.

### Configuration

**Environment Variables:**

| Lambda Function | Variable | Value |
|----------------|----------|-------|
| IngestHandler | `QUEUE_URL` | SQS queue URL |
| WorkerHandler | `TABLE_NAME` | `ProcessedLogs` |

**SQS Settings:**
- Visibility Timeout: 120 seconds (2x Lambda timeout)
- Batch Size: 10 messages per Lambda invocation

**Lambda Settings:**
- Runtime: Java 17
- IngestHandler: 512 MB memory, 30s timeout
- WorkerHandler: 512 MB memory, 60s timeout

## 📡 API Usage

### Endpoint
```
POST https://<API_ID>.execute-api.us-east-1.amazonaws.com/prod/ingest
```

### Scenario 1: JSON Payload

**Request:**
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "tenant_id": "acme",
    "log_id": "123",
    "text": "User 555-0199 accessed system at 10:30 AM"
  }'
```

**Response (202 Accepted):**
```json
{
  "message": "Accepted",
  "log_id": "123"
}
```

**DynamoDB Result:**
```
PK: TENANT#acme
SK: LOG#123
source: "json"
original_text: "User 555-0199 accessed system at 10:30 AM"
modified_data: "User [REDACTED] accessed system at 10:30 AM"
processed_at: "2025-11-30T01:23:45Z"
```

### Scenario 2: Plain Text

**Request:**
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: text/plain" \
  -H "X-Tenant-ID: beta_inc" \
  -d "Raw log entry: Customer called 555-1234 for support"
```

**Response (202 Accepted):**
```json
{
  "message": "Accepted",
  "log_id": "a3f5c8e2-4b9d-11ec-81d3-0242ac130003"
}
```

**DynamoDB Result:**
```
PK: TENANT#beta_inc
SK: LOG#a3f5c8e2-4b9d-11ec-81d3-0242ac130003
source: "text_upload"
original_text: "Raw log entry: Customer called 555-1234 for support"
modified_data: "Raw log entry: Customer called [REDACTED] for support"
processed_at: "2025-11-30T01:25:12Z"
```

### Error Responses

**Missing tenant_id (400 Bad Request):**
```json
{
  "error": "Missing required fields: tenant_id and text"
}
```

**Missing X-Tenant-ID header for text (400 Bad Request):**
```json
{
  "error": "Missing X-Tenant-ID header"
}
```

**Unsupported Content-Type (400 Bad Request):**
```json
{
  "error": "Unsupported Content-Type. Use application/json or text/plain"
}
```

## 🧪 Testing

### Functional Test

Test both input formats:
```bash
# JSON
curl -X POST <API_URL>/ingest \
  -H "Content-Type: application/json" \
  -d '{"tenant_id":"test_tenant","log_id":"1","text":"Test message"}'

# Plain Text
curl -X POST <API_URL>/ingest \
  -H "Content-Type: text/plain" \
  -H "X-Tenant-ID: test_tenant" \
  -d "Plain text test message"
```

Verify in DynamoDB that both entries appear under `TENANT#test_tenant`.

### Load Test

**Send 100 concurrent requests:**
```bash
for i in {1..100}; do
  curl -X POST <API_URL>/ingest \
    -H "Content-Type: application/json" \
    -d "{\"tenant_id\":\"acme\",\"log_id\":\"load$i\",\"text\":\"Load test message number $i with sufficient text to simulate processing time\"}" &
done
wait
```

**Expected Results:**
- All requests return 202 Accepted within ~100-200ms
- Check CloudWatch Metrics:
  - IngestHandler ConcurrentExecutions: 1-3 (very fast, doesn't need much scaling)
  - WorkerHandler ConcurrentExecutions: 6-10 (slower, scales to handle load)
- All 100 entries appear in DynamoDB within ~30-60 seconds

### Multi-Tenant Test

Verify data isolation:
```bash
# Send logs for tenant A
curl -X POST <API_URL>/ingest -H "Content-Type: application/json" \
  -d '{"tenant_id":"tenant_a","log_id":"1","text":"Tenant A data"}'

# Send logs for tenant B
curl -X POST <API_URL>/ingest -H "Content-Type: application/json" \
  -d '{"tenant_id":"tenant_b","log_id":"1","text":"Tenant B data"}'
```

In DynamoDB, verify:
- `PK: TENANT#tenant_a` contains only Tenant A's logs
- `PK: TENANT#tenant_b` contains only Tenant B's logs
- No cross-contamination between tenants

## 📈 Monitoring & Observability

### CloudWatch Metrics

**Key Metrics to Monitor:**

| Metric | Lambda | What It Shows |
|--------|--------|---------------|
| Invocations | Both | Total function calls |
| ConcurrentExecutions | Both | Instances running in parallel |
| Duration | Both | Execution time per invocation |
| Errors | Both | Failed invocations |
| Throttles | Both | Rate-limited requests (should be 0) |

**Typical Values Under Load:**
- IngestHandler ConcurrentExecutions: 1-3 (fast processing)
- WorkerHandler ConcurrentExecutions: 5-15 (slower, more parallel instances needed)

### CloudWatch Logs

View detailed execution logs:
```bash
# IngestHandler logs
aws logs tail /aws/lambda/IngestHandler --follow

# WorkerHandler logs  
aws logs tail /aws/lambda/WorkerHandler --follow
```

### SQS Monitoring

- **ApproximateNumberOfMessagesVisible**: Messages waiting to be processed
- **ApproximateAgeOfOldestMessage**: How long oldest message has been waiting
- **NumberOfMessagesReceived**: Total messages received

## 🔒 Security & Best Practices

### IAM Least Privilege

Lambda execution role has only required permissions:
- CloudWatch Logs write access
- SQS send/receive for specific queue
- DynamoDB read/write for specific table

### Data Privacy

- Phone numbers automatically redacted: `555-1234` → `[REDACTED]`
- Can be extended to redact emails, SSNs, credit cards, etc.

### Crash Resilience

**SQS Visibility Timeout Mechanism:**
1. Lambda receives message → Message becomes invisible (120s)
2. Lambda processes successfully → Deletes message
3. Lambda crashes → Message becomes visible again after 120s → Retried

**Configuration:**
- SQS Visibility Timeout: 120 seconds
- Lambda Timeout: 60 seconds
- Ratio: 2:1 (prevents duplicate processing)

## 🎯 Design Decisions

### Why Lambda?
- Auto-scaling from 0 to 1000+ instances
- Pay only for execution time
- No server management

### Why SQS?
- Decouples fast API from slow processing
- Built-in retry mechanism
- Messages persist until successfully processed

### Why DynamoDB?
- Serverless (no database servers to manage)
- Auto-scaling read/write capacity
- Native support for composite keys (perfect for multi-tenancy)

### Why Async Processing?
- API responds in ~100ms regardless of processing time
- Better user experience
- System can handle traffic spikes (queue absorbs burst)

## 👤 Author

Varnika Mujumdar

---
