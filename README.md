# 🚀 AWS Serverless Log Processor

> A multi-tenant log processing system that handles high-throughput data ingestion with async processing and strict tenant isolation.

---

## 💡 What This Does

Ever wondered how to handle **1000+ log entries per minute** without bottlenecking your API? This system does exactly that!

- 🎯 **REST API** accepts logs (JSON or plain text)
- ⚡ **Returns 202 in ~100ms** (doesn't wait for processing)
- 🔄 **Processes asynchronously** in the background (2-10 seconds)
- 🔒 **Strict tenant isolation** in DynamoDB

**TL;DR:** Fast ingestion + Slow processing = Happy users 😎

---

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

---

## 🌊 Request Flow

1. **Client** sends `POST /ingest` (JSON or text)
2. **API Gateway** → IngestHandler Lambda
3. **IngestHandler** validates & sends to SQS Queue
4. **API returns 202 Accepted** immediately ⚡
5. **SQS** triggers WorkerHandler Lambda
6. **WorkerHandler** processes & saves to DynamoDB
7. **Data stored** with tenant isolation (`PK: TENANT#<tenant_id>`)

---

## 📂 Project Structure
```
log-processor-aws/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── logprocessor/
│                   ├── handler/
│                   │   ├── IngestHandler.java       # 🚪 API Lambda - receives requests
│                   │   └── WorkerHandler.java       # ⚙️  Worker Lambda - processes async
│                   └── model/
│                       ├── LogData.java             # 📦 Request model
│                       └── ProcessedLog.java        # 💾 Database model
├── pom.xml                                          # 📦 Maven dependencies
├── trust-policy.json                                # 🔐 IAM trust policy
└── README.md
```

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| 💻 Language | Java 17 + Maven |
| ⚡ Compute | AWS Lambda (serverless) |
| 🌐 API | AWS API Gateway (REST) |
| 📬 Queue | AWS SQS (message buffer) |
| 💾 Database | AWS DynamoDB (NoSQL) |

---

## 🎯 API Usage

### 📝 JSON Request
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: application/json" \
  -d '{"tenant_id":"acme","log_id":"123","text":"User accessed system"}'
```

### 📄 Plain Text Request
```bash
curl -X POST <API_URL>/ingest \
  -H "Content-Type: text/plain" \
  -H "X-Tenant-ID: acme" \
  -d "Raw log text here"
```

### ✅ Response
```json
{"message":"Accepted","log_id":"123"}
```

---

## 🔥 Load Test (1000 Requests)

Want to see auto-scaling in action? Fire away! 💪
```bash
for i in {1..1000}; do
  curl -X POST <API_URL>/ingest \
    -H "Content-Type: application/json" \
    -d "{\"tenant_id\":\"acme\",\"log_id\":\"test$i\",\"text\":\"Load test message $i\"}" &
done
wait
```

**What happens?**
- 🚀 All requests return 202 in ~100ms
- 📈 Lambda auto-scales to 6-10 workers
- 💾 All 1000 logs processed and stored

---

## 🔒 Multi-Tenant Isolation

Each customer's data is **physically separated** in DynamoDB:
```
PK: TENANT#acme       → 🏢 Customer A's logs
PK: TENANT#beta_inc   → 🏭 Customer B's logs
```

---

## 👩‍💻 Author

**Varnika Mujumdar**

---
