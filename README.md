# 💳 Payment Gateway with Multi-Method Processing & Hosted Checkout

A full-stack payment gateway simulation built with **Java Spring Boot**, **React**, **PostgreSQL**, **Redis**, and **Docker**. This system mimics a real-world payment infrastructure, featuring a secure REST API, async payment processing, webhook delivery, a merchant dashboard, and an embeddable JavaScript SDK for hosted checkout.

---

## 🚀 Features

### Core Payment Engine
- REST API for creating Orders and processing Payments
- **Async Processing** with Redis job queues and background workers
- Strategy Pattern for multiple payment methods (**UPI** & **Credit/Debit Card**)
- **Simulation Logic:** Mimics real banking delays (5-10s) and configurable success/failure rates

### Advanced Features
- **Idempotency Keys** - Prevent duplicate payment processing with 24-hour cached responses
- **Payment Capture** - Two-step authorization and capture flow
- **Refunds API** - Full and partial refunds with validation
- **Webhook System** - Real-time event notifications with HMAC-SHA256 signatures
- **Exponential Backoff Retry** - Automatic webhook retries with configurable intervals

### Security
- `X-Api-Key` and `X-Api-Secret` authentication for Merchant APIs
- HMAC-SHA256 webhook signature verification
- CORS configuration for secure frontend communication

### Merchant Dashboard (Port 3000)
- View API Credentials (Key ID & Secret)
- Real-time analytics (Total Volume, Success Rate)
- Transaction history table
- **Webhook Configuration** - Set endpoint URL, view/copy secret
- **Webhook Logs** - Event history with retry functionality
- **API Documentation** - Integration guide with code examples

### Hosted Checkout (Port 3001)
- Secure payment form for customers
- Real-time polling of payment status
- Auto-redirect on success/failure
- **Embeddable JavaScript SDK** (`checkout.js`) for iframe modal integration

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 17, Spring Boot 3.x, Maven |
| **Database** | PostgreSQL 15 |
| **Cache/Queue** | Redis 7 |
| **Frontend** | React 18 (Vite), Axios |
| **Infrastructure** | Docker, Docker Compose, Nginx |

---

## 📂 Project Structure

```text
payment-gateway/
├── backend/                 # Spring Boot API & Worker
│   ├── src/main/java/com/gateway/
│   │   ├── config/          # Security, CORS, Redis config
│   │   ├── controllers/     # REST API Endpoints
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entities/        # JPA Entities (Refund, WebhookLog, etc.)
│   │   ├── models/          # Domain Models (Payment, Order, Merchant)
│   │   ├── repositories/    # Database Access
│   │   ├── services/        # Business Logic & Webhook Service
│   │   ├── workers/         # Background Job Processors
│   │   └── jobs/            # Job Definitions
│   └── Dockerfile
├── frontend/                # Merchant Dashboard (React)
│   ├── src/
│   │   ├── pages/           # Dashboard, Webhooks, Docs pages
│   │   └── Checkout.jsx     # Checkout component
│   └── Dockerfile
├── checkout-page/           # Hosted Checkout & SDK
│   ├── public/checkout.js   # Embeddable JavaScript SDK
│   ├── src/
│   └── Dockerfile
├── checkout-widget/         # SDK Development (Webpack)
└── docker-compose.yml
```

---

## ⚙️ Prerequisites

- **Docker Desktop** (running)

No local Java or Node installation required.

---

## 🏃‍♂️ How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/Shamshuu/Payment-Gateway-with-Async-Processing-and-Webhooks
cd Payment Gateway with Async Processing and Webhooks
```

### 2. Start All Services

```bash
docker-compose up -d --build
```

### 3. Verify Containers

```bash
docker ps
```

You should see 6 containers running:
- `pg_gateway` (PostgreSQL)
- `redis_gateway` (Redis)
- `gateway_api` (Backend API)
- `gateway_worker` (Background Worker)
- `gateway_frontend` (Dashboard)
- `gateway_checkout` (Checkout Page)

---

## 🔗 Access Points

| Service | URL | Description |
|---------|-----|-------------|
| Merchant Dashboard | http://localhost:3000 | Analytics, API keys, webhooks |
| Checkout Page | http://localhost:3001 | Customer payment form |
| Backend API | http://localhost:8000 | REST API endpoints |
| SDK Script | http://localhost:3001/checkout.js | Embeddable SDK |
| Database | localhost:5432 | PostgreSQL (internal) |
| Redis | localhost:6379 | Queue/Cache (internal) |

---

## 🔑 Test Credentials

A test merchant is automatically created on startup:

| Field | Value |
|-------|-------|
| Email | test@example.com |
| API Key | key_test_abc123 |
| API Secret | secret_test_xyz789 |

---

## 📡 API Reference

### Authentication
All API calls require these headers:
```
X-Api-Key: your_api_key
X-Api-Secret: your_api_secret
Content-Type: application/json
```

### Endpoints

#### Create Order
```bash
POST /api/v1/orders
{
  "amount": 5000,
  "currency": "INR",
  "receipt": "order_001"
}
```

#### Create Payment (Async)
```bash
POST /api/v1/payments
Idempotency-Key: unique_key_123  # Optional
{
  "order_id": "order_xxx",
  "method": "upi",
  "vpa": "customer@upi",
  "amount": 5000,
  "currency": "INR"
}
```
Returns immediately with `"status": "pending"`. Status changes to `success` or `failed` after 5-10 seconds.

#### Get Payment Status
```bash
GET /api/v1/payments/{payment_id}/public
```

#### Capture Payment
```bash
POST /api/v1/payments/{payment_id}/capture
{
  "amount": 5000
}
```

#### Create Refund
```bash
POST /api/v1/payments/{payment_id}/refunds
{
  "amount": 2500,
  "reason": "Customer requested"
}
```

#### Get Refund Status
```bash
GET /api/v1/refunds/{refund_id}
```

#### Get Webhook Logs
```bash
GET /api/v1/webhooks?limit=10
```

#### Retry Webhook
```bash
POST /api/v1/webhooks/{webhook_log_id}/retry
```

#### Job Queue Status (No Auth Required)
```bash
GET /api/v1/test/jobs/status
```

---

## 🔌 SDK Integration

### Include the SDK
```html
<script src="http://localhost:3001/checkout.js"></script>
```

### Initialize and Open Modal
```javascript
const checkout = new PaymentGateway({
  key: 'key_test_abc123',
  orderId: 'order_xxx',  // From Create Order API
  onSuccess: function(response) {
    console.log('Payment ID:', response.paymentId);
  },
  onFailure: function(error) {
    console.error('Error:', error.error);
  },
  onClose: function() {
    console.log('Modal closed');
  }
});

checkout.open();
```

---

## 🪝 Webhook Integration

### Configure Webhook URL
Set via Dashboard → Webhooks, or directly in database:
```sql
UPDATE merchants SET webhook_url = 'https://your-server.com/webhook' WHERE email = 'test@example.com';
```

### Events Sent
- `payment.success` - Payment completed successfully
- `payment.failed` - Payment failed
- `refund.processed` - Refund completed

### Signature Verification (Node.js)
```javascript
const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expected = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
  return signature === expected;
}

// Usage in Express
app.post('/webhook', (req, res) => {
  const signature = req.headers['x-webhook-signature'];
  const isValid = verifyWebhook(req.body, signature, 'your_webhook_secret');
  
  if (isValid) {
    console.log('Event:', req.body.event);
    res.status(200).send('OK');
  } else {
    res.status(401).send('Invalid signature');
  }
});
```

### Retry Schedule (Production)
| Attempt | Delay |
|---------|-------|
| 1 | 60 seconds |
| 2 | 5 minutes |
| 3 | 30 minutes |
| 4 | 2 hours |
| 5 | Failed (no more retries) |

---

## 🧪 End-to-End Testing

### Step 1: Register/Login
Go to http://localhost:3000 and create an account or use test credentials.

### Step 2: Create Order
```bash
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: key_test_abc123" \
  -H "X-Api-Secret: secret_test_xyz789" \
  -d '{"amount": 5000, "currency": "INR", "receipt": "test_001"}'
```

### Step 3: Complete Payment
Open: `http://localhost:3001/checkout?order_id=ORDER_ID_FROM_STEP_2`

Choose UPI or Card, enter details, and click Pay.

### Step 4: Verify in Dashboard
- Check **Transactions** tab for the new payment
- Check **Webhooks** tab for delivery logs

---

## ⚙️ Configuration

Environment variables in `docker-compose.yml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `UPI_SUCCESS_RATE` | 0.90 | UPI payment success probability |
| `CARD_SUCCESS_RATE` | 0.95 | Card payment success probability |
| `PROCESSING_DELAY_MIN` | 5000 | Minimum processing delay (ms) |
| `PROCESSING_DELAY_MAX` | 10000 | Maximum processing delay (ms) |
| `TEST_MODE` | false | Enable shorter webhook retry intervals |

---

## �️ Database Schema

### Key Tables
- **merchants** - Merchant accounts with API keys and webhook config
- **orders** - Order records
- **payments** - Payment transactions with status, capture flag, error details
- **refunds** - Refund records with processing status
- **webhook_logs** - Webhook delivery attempts and retry tracking
- **idempotency_keys** - Request deduplication cache

---

## �🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| CORS errors | Verify backend CORS config allows frontend origin |
| Stuck on "processing" | Check `gateway_worker` container logs |
| Database connection | Restart `gateway_api` container |
| Webhooks not received | Ensure URL is accessible from Docker (use `host.docker.internal`) |
| 500 errors | Check `docker logs gateway_api` for stack traces |

### View Logs
```bash
docker logs gateway_api      # API logs
docker logs gateway_worker   # Worker logs
docker logs pg_gateway       # Database logs
```

### Reset Database
```bash
docker-compose down -v
docker-compose up -d --build
```

---