# AWS Serverless Log Processor

A multi-tenant log processing system that handles high-throughput data ingestion with async processing and strict tenant isolation.

## What This Does

Accepts log data via REST API, processes it asynchronously, and stores it in DynamoDB with tenant isolation. API returns immediately (~100ms) while processing happens in background (2-10 seconds).

## Architecture
```
┌──────────┐     ┌──────────────┐     ┌─────────────────┐     ┌─────────┐     ┌──────────────────┐     ┌──────────┐
│  Client  │────▶│ API Gateway  │────▶│ IngestHandler   │────▶│   SQS   │────▶│ WorkerHandler    │────▶│ DynamoDB │
│          │     │  (REST API)  │     │    (Lambda)     │     │  Queue  │     │    (Lambda)      │     │          │
└──────────┘     └──────────────┘     └─────────────────┘     └─────────┘     └──────────────────┘     └──────────┘
                                              │                                         │
                                       Returns 202 Accepted                      Async Processing
                                       (~100ms response)                         (2-10s per message)
```

## Request Flow

1. Client sends POST /ingest (JSON or text)
2. API Gateway → IngestHandler Lambda
3. IngestHandler validates & sends to SQS Queue
4. API returns 202 Accepted immediately
5. SQS triggers WorkerHandler Lambda
6. WorkerHandler processes & saves to DynamoDB
7. Data stored with tenant isolation (PK: TENANT#<tenant_id>)

## Project Structure
```
src/main/java/com/logprocessor/
├── handler/
│   ├── IngestHandler.java       # API Lambda - receives requests
│   └── WorkerHandler.java       # Worker Lambda - processes async
└── model/
    ├── LogData.java             # Request model
    └── ProcessedLog.java        # Database model
```

## Tech Stack

- Java 17 + Maven
- AWS Lambda (serverless compute)
- AWS API Gateway (REST API)
- AWS SQS (message queue)
- AWS DynamoDB (database)

## API Usage

**JSON Request:**
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: application/json" \
  -d '{"tenant_id":"acme","log_id":"123","text":"User accessed system"}'
```

**Plain Text Request:**
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: text/plain" \
  -H "X-Tenant-ID: acme" \
  -d "Raw log text here"
```

**Response:**
```json
{"message":"Accepted","log_id":"123"}
```

## Load Test (1000 Requests)
```bash
for i in {1..1000}; do
  curl -X POST <API_URL>/ingest \
    -H "Content-Type: application/json" \
    -d "{\"tenant_id\":\"acme\",\"log_id\":\"test$i\",\"text\":\"Load test message $i\"}" &
done
wait
```

## Multi-Tenant Isolation

Each tenant's data is separated in DynamoDB:
```
PK: TENANT#acme       → Customer A's logs
PK: TENANT#beta_inc   → Customer B's logs
```

## Author

Varnika Mujumdar
