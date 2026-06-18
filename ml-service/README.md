# SEIP ML Fraud Detection Service

A FastAPI-based Machine Learning microservice that predicts the fraud probability of expense claims submitted through the **Smart Expense Intelligence Platform (SEIP)**.

---

## Architecture Overview

```
Fraud Service (Java)
       │
       │  POST /ml/predict-fraud
       ▼
ML Service (Python / FastAPI)  ←── Port 8088
       │
       │  Returns fraud_probability (0.0 – 1.0) + risk_level
       ▼
Fraud Service → flags expense in DB + emits Kafka event
```

---

## Phase 1 — Heuristic Rule-Enhanced Scorer (Current)

The current implementation uses a weighted heuristic scoring model that combines five feature signals:

| Feature | Weight | Description |
|---|---|---|
| `amount` | **35%** | Higher amounts are inherently riskier |
| `category_code` | **20%** | MISC / ENTERTAINMENT carry the highest inherent risk |
| `monthly_claim_count` | **20%** | Burst of claims in a month is suspicious |
| `employee_history_score` | **15%** | Low historical trust score increases risk |
| `is_weekend` | **10%** | Weekend submissions are statistically more anomalous |

### Category Risk Table

| Code | Risk Score |
|---|---|
| MISC | 0.80 |
| ENTERTAINMENT | 0.65 |
| TRAVEL | 0.30 |
| ACCOM | 0.25 |
| MEALS | 0.20 |
| MEDICAL | 0.20 |
| OFFICE | 0.15 |
| EQUIPMENT | 0.15 |
| SUBSCRIPTION | 0.12 |
| SOFTWARE | 0.10 |
| TRAINING | 0.10 |

### Risk Level Thresholds

| `fraud_probability` | `risk_level` |
|---|---|
| > 0.70 | `HIGH` |
| 0.35 – 0.70 | `MEDIUM` |
| < 0.35 | `LOW` |

---

## Phase 2 — Isolation Forest + Random Forest Ensemble (Planned)

Phase 2 will replace the heuristic with a trained ensemble:

### Algorithm Design

```
Raw Expense Features
        │
        ├── Isolation Forest   (anomaly detection, unsupervised)
        │       └── anomaly_score ──────────────────────────────┐
        │                                                        │
        └── Feature Engineering                                  │
                ├── amount_zscore (vs. employee category avg)    │
                ├── temporal_features (hour, day, month)         │
                ├── merchant_embedding (word2vec on merchant)    │
                └── employee_peer_deviation                      │
                        │                                        │
                        ▼                                        │
               Random Forest Classifier ◄────────────────────────┘
                        │
                        ▼
              fraud_probability (calibrated with Platt scaling)
```

### Training Plan

- **Dataset**: Synthetic expense history (100k records, 8% fraud rate)
- **Validation**: Stratified 5-fold cross-validation
- **Target metrics**: AUC-ROC ≥ 0.92, Precision@90%Recall ≥ 0.75
- **Retraining**: Monthly batch retraining via MLflow + DVC pipeline
- **Model registry**: MLflow Model Registry → promote to `Production` stage on approval

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Kubernetes liveness/readiness probe |
| `POST` | `/ml/predict-fraud` | Single expense fraud prediction |
| `POST` | `/ml/predict-fraud/batch` | Batch prediction (up to 100 expenses) |
| `GET` | `/ml/model-info` | Active model metadata |
| `GET` | `/metrics` | Prometheus metrics endpoint |
| `GET` | `/docs` | Swagger UI |
| `GET` | `/redoc` | ReDoc API docs |

---

## Request / Response

### `POST /ml/predict-fraud`

**Request:**
```json
{
  "expense_id": 1001,
  "amount": 4500.00,
  "category_code": "MISC",
  "employee_id": 42,
  "monthly_claim_count": 7,
  "frequency_score": 0.7,
  "employee_history_score": 0.3,
  "day_of_week": 6,
  "is_weekend": true,
  "merchant_name": "Unknown Vendor Ltd"
}
```

**Response:**
```json
{
  "expense_id": 1001,
  "fraud_probability": 0.8241,
  "risk_level": "HIGH",
  "model": "isolation-forest-heuristic-v1",
  "features_used": ["amount", "category_code", "monthly_claim_count", "history_score", "weekend"],
  "prediction_time": "2024-09-15T10:23:44.123Z"
}
```

---

## Local Development

```bash
# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the service
uvicorn main:app --reload --port 8088

# Open API docs
open http://localhost:8088/docs
```

## Docker

```bash
docker build -t seip-ml-service .
docker run -p 8088:8088 seip-ml-service
```

---

## Observability

The service exposes Prometheus metrics at `/metrics` via `prometheus-fastapi-instrumentator`:

- `http_requests_total` — total request count by method, path, status
- `http_request_duration_seconds` — request latency histogram
- `http_requests_in_progress` — current in-flight requests

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8088` | Service port |
| `LOG_LEVEL` | `info` | Uvicorn log level |
| `WORKERS` | `2` | Number of Uvicorn worker processes |
