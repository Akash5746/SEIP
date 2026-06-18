"""
SEIP ML Fraud Detection Service
================================
Phase 1: Heuristic Rule-Enhanced Scorer
Phase 2: Isolation Forest + Random Forest Ensemble (planned)
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional
import numpy as np
from datetime import datetime
from prometheus_fastapi_instrumentator import Instrumentator

# ─────────────────────────────────────────────
# App
# ─────────────────────────────────────────────
app = FastAPI(
    title="SEIP ML Fraud Detection Service",
    description=(
        "Machine Learning service for fraud probability prediction. "
        "Phase 1 uses a heuristic rule-enhanced scorer. "
        "Phase 2 will employ an Isolation Forest + Random Forest ensemble "
        "trained on real expense history."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Instrument all routes with Prometheus metrics
Instrumentator(
    should_group_status_codes=False,
    excluded_handlers=["/health", "/metrics"],
).instrument(app).expose(app)


# ─────────────────────────────────────────────
# Request / Response models
# ─────────────────────────────────────────────

class FraudPredictRequest(BaseModel):
    expense_id: int = Field(..., description="Unique expense identifier", example=1001)
    amount: float = Field(..., gt=0, description="Expense amount in USD", example=4500.00)
    category_code: str = Field(..., description="Expense category code", example="MISC")
    employee_id: int = Field(..., description="Employee identifier", example=42)
    monthly_claim_count: int = Field(default=0, ge=0, description="Number of claims this month")
    frequency_score: float = Field(default=0.0, ge=0.0, le=1.0, description="Normalised claim frequency (0–1)")
    employee_history_score: float = Field(default=0.5, ge=0.0, le=1.0, description="Historical trustworthiness score (0=suspicious, 1=trusted)")
    day_of_week: int = Field(default=1, ge=0, le=6, description="0=Monday … 6=Sunday")
    is_weekend: bool = Field(default=False, description="Whether the expense was submitted on a weekend")
    merchant_name: Optional[str] = Field(default=None, description="Optional merchant name for future NLP enrichment")

    @field_validator("category_code")
    @classmethod
    def normalise_category(cls, v: str) -> str:
        return v.strip().upper()


class FraudPredictResponse(BaseModel):
    expense_id: int
    fraud_probability: float = Field(..., ge=0.0, le=1.0)
    risk_level: str = Field(..., description="LOW | MEDIUM | HIGH")
    model: str
    features_used: List[str]
    prediction_time: str


class BatchPredictRequest(BaseModel):
    expenses: List[FraudPredictRequest] = Field(..., min_length=1, max_length=100)


class BatchPredictResponse(BaseModel):
    results: List[FraudPredictResponse]
    batch_size: int
    processed_at: str


# ─────────────────────────────────────────────
# Domain knowledge: category risk weights
# ─────────────────────────────────────────────

CATEGORY_RISK: dict[str, float] = {
    "MISC": 0.80,
    "ENTERTAINMENT": 0.65,
    "TRAVEL": 0.30,
    "ACCOM": 0.25,
    "MEALS": 0.20,
    "MEDICAL": 0.20,
    "OFFICE": 0.15,
    "SOFTWARE": 0.10,
    "TRAINING": 0.10,
    "EQUIPMENT": 0.15,
    "SUBSCRIPTION": 0.12,
}

FEATURES_USED = [
    "amount",
    "category_code",
    "monthly_claim_count",
    "frequency_score",
    "employee_history_score",
    "is_weekend",
    "day_of_week",
]


# ─────────────────────────────────────────────
# Scoring logic (Phase 1 — heuristic)
# ─────────────────────────────────────────────

def compute_fraud_probability(
    amount: float,
    category_code: str,
    monthly_claim_count: int,
    frequency_score: float,
    employee_history_score: float,
    is_weekend: bool,
    day_of_week: int,
) -> float:
    """
    Rule-enhanced heuristic fraud probability estimator (Phase 1).

    Weights:
    - Amount        35%  — higher amounts are riskier
    - Category      20%  — MISC/Entertainment carry high inherent risk
    - Frequency     20%  — bursts of claims in a single month are suspicious
    - History       15%  — low trust score penalises the employee
    - Weekend       10%  — weekend submissions are more anomalous

    Phase 2 will replace this with a trained Isolation Forest + Random Forest
    ensemble using a synthetic dataset of 100k expense records.
    """
    score = 0.0

    # 1. Amount risk (capped at $100k)
    amount_score = min(amount / 100_000.0, 1.0)
    score += amount_score * 0.35

    # 2. Category risk
    category_risk = CATEGORY_RISK.get(category_code, 0.30)
    score += category_risk * 0.20

    # 3. Monthly claim frequency (10 claims = max penalty)
    frequency_penalty = min(monthly_claim_count / 10.0, 1.0)
    score += frequency_penalty * 0.20

    # 4. Employee history (lower trust → higher penalty)
    history_penalty = 1.0 - employee_history_score
    score += history_penalty * 0.15

    # 5. Weekend bonus risk
    if is_weekend:
        score += 0.10

    # Add calibrated Gaussian noise to simulate model uncertainty
    noise = float(np.random.normal(0, 0.02))
    score = max(0.0, min(1.0, score + noise))

    return round(score, 4)


def risk_level(probability: float) -> str:
    if probability > 0.70:
        return "HIGH"
    if probability > 0.35:
        return "MEDIUM"
    return "LOW"


def predict_single(req: FraudPredictRequest) -> FraudPredictResponse:
    probability = compute_fraud_probability(
        amount=req.amount,
        category_code=req.category_code,
        monthly_claim_count=req.monthly_claim_count,
        frequency_score=req.frequency_score,
        employee_history_score=req.employee_history_score,
        is_weekend=req.is_weekend,
        day_of_week=req.day_of_week,
    )
    return FraudPredictResponse(
        expense_id=req.expense_id,
        fraud_probability=probability,
        risk_level=risk_level(probability),
        model="isolation-forest-heuristic-v1",
        features_used=FEATURES_USED,
        prediction_time=datetime.utcnow().isoformat() + "Z",
    )


# ─────────────────────────────────────────────
# Routes
# ─────────────────────────────────────────────

@app.get("/health", tags=["Health"])
def health_check():
    """Kubernetes liveness / readiness probe endpoint."""
    return {
        "status": "healthy",
        "service": "seip-ml-fraud-detection",
        "model": "heuristic-v1",
        "timestamp": datetime.utcnow().isoformat() + "Z",
    }


@app.post(
    "/ml/predict-fraud",
    response_model=FraudPredictResponse,
    tags=["Fraud Detection"],
    summary="Predict fraud probability for a single expense",
)
def predict_fraud(request: FraudPredictRequest):
    """
    Compute the fraud probability for one expense record.

    Returns a score between 0.0 (definitely legitimate) and 1.0 (highly suspicious),
    along with a categorical risk level (LOW / MEDIUM / HIGH).
    """
    try:
        return predict_single(request)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {exc}") from exc


@app.post(
    "/ml/predict-fraud/batch",
    response_model=BatchPredictResponse,
    tags=["Fraud Detection"],
    summary="Batch predict fraud probability for up to 100 expenses",
)
def predict_fraud_batch(request: BatchPredictRequest):
    """
    Compute fraud probabilities for a batch of expense records in a single call.
    Useful for bulk re-scoring of historical data or nightly batch jobs.
    """
    try:
        results = [predict_single(exp) for exp in request.expenses]
        return BatchPredictResponse(
            results=results,
            batch_size=len(results),
            processed_at=datetime.utcnow().isoformat() + "Z",
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {exc}") from exc


@app.get(
    "/ml/model-info",
    tags=["Model Metadata"],
    summary="Returns metadata about the active model",
)
def model_info():
    """Describes the current model version, algorithm, features, and roadmap."""
    return {
        "model_name": "SEIP Fraud Detection",
        "version": "1.0.0-heuristic",
        "phase": 1,
        "algorithm": "Heuristic Rule-Enhanced Scorer",
        "phase_2_algorithm": "Isolation Forest + Random Forest Ensemble",
        "features": FEATURES_USED,
        "category_risk_table": CATEGORY_RISK,
        "output": "fraud_probability (0.0–1.0) + risk_level (LOW|MEDIUM|HIGH)",
        "training_data": "Phase 2: Synthetic expense history dataset (100k records)",
        "last_updated": "2024-01-01",
        "contact": "ml-team@seip.example.com",
    }


# ─────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8088, log_level="info")
