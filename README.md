# Smart Expense Intelligence Platform (SEIP)

> **Production-grade microservices expense management platform** with fraud detection, ML scoring, real-time notifications, audit trail, and full observability.

---

## Architecture

```
                          ┌─────────────────────────────────────────────────────────────┐
                          │                    seip-network (Docker bridge)              │
                          │                                                             │
   Browser / Mobile       │   ┌──────────┐    ┌─────────────────────────────────────┐  │
         │                │   │ Frontend │    │           API Gateway               │  │
         │  :3000         │   │  React   │───▶│        seip-api-gateway:8080        │  │
         └───────────────▶│   │  :3000   │    └────────────────┬────────────────────┘  │
                          │   └──────────┘                     │                       │
                          │                          ┌──────────▼──────────┐           │
                          │                          │   Core Services     │           │
                          │   ┌─────────────────┐    │                     │           │
                          │   │  Auth  :8081    │◀───│  Route by /api/*    │           │
                          │   │  User  :8082    │    │                     │           │
                          │   │  Expense:8083   │    └─────────────────────┘           │
                          │   │  Fraud  :8084   │                                      │
                          │   │  Notify :8085   │──┐                                   │
                          │   │  Audit  :8086   │  │  Kafka Events                     │
                          │   │  Analytics:8087 │  │                                   │
                          │   │  ML    :8088    │  │  ┌──────────────────────────────┐ │
                          │   └────────┬────────┘  └─▶│    seip-kafka:29092          │ │
                          │            │               └──────────────────────────────┘ │
                          │            │                                                │
                          │   ┌────────▼────────────────────────┐                      │
                          │   │         Infrastructure           │                      │
                          │   │  PostgreSQL  :5432              │                      │
                          │   │  MinIO       :9000              │                      │
                          │   │  Elasticsearch:9200             │                      │
                          │   └─────────────────────────────────┘                      │
                          │                                                             │
                          │   ┌─────────────────────────────────┐                      │
                          │   │          Observability           │                      │
                          │   │  Prometheus  :9090              │                      │
                          │   │  Grafana     :3001              │                      │
                          │   │  Logstash    :5000              │                      │
                          │   └─────────────────────────────────┘                      │
                          └─────────────────────────────────────────────────────────────┘
```

---

## Services

| Service | Container | Port | Technology |
|---|---|---|---|
| API Gateway | `seip-api-gateway` | 8080 | Spring Cloud Gateway |
| Auth Service | `seip-auth` | 8081 | Spring Boot + JWT |
| User Service | `seip-user` | 8082 | Spring Boot |
| Expense Service | `seip-expense` | 8083 | Spring Boot + MinIO |
| Fraud Service | `seip-fraud` | 8084 | Spring Boot + Kafka |
| Notification Service | `seip-notification` | 8085 | Spring Boot + SMTP |
| Audit Service | `seip-audit` | 8086 | Spring Boot + Kafka |
| Analytics Service | `seip-analytics` | 8087 | Spring Boot |
| ML Service | `seip-ml` | 8088 | FastAPI + scikit-learn |
| React Frontend | `seip-frontend` | 3000 | React + TypeScript |

## Infrastructure

| Component | Container | Port |
|---|---|---|
| PostgreSQL 16 | `seip-postgres` | 5432 |
| Apache Kafka | `seip-kafka` | 9092 |
| Zookeeper | `seip-zookeeper` | 2181 |
| MinIO | `seip-minio` | 9000 / 9001 |
| Elasticsearch | `seip-elasticsearch` | 9200 |
| Prometheus | `seip-prometheus` | 9090 |
| Grafana | `seip-grafana` | 3001 |
| Logstash | `seip-logstash` | 5000 |

---

## Prerequisites

| Tool | Minimum Version |
|---|---|
| Docker Desktop | 24.x |
| Docker Compose | v2.x |
| Java (JDK) | 21 (Temurin) |
| Maven | 3.9.x (or use `./mvnw`) |
| Node.js | 20 LTS |
| Python | 3.11 |
| kubectl | 1.29+ (Kubernetes only) |

---

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/your-org/smart-expense-platform.git
cd smart-expense-platform
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env and set your MAIL_USER, MAIL_PASSWORD, etc.
```

### 3. Start infrastructure

```bash
docker compose up -d postgres zookeeper kafka minio elasticsearch
# Wait ~30s for Postgres and Kafka to be healthy
docker compose ps
```

### 4. Build all Java services

```bash
./mvnw clean package -DskipTests
# Windows:
mvnw.cmd clean package -DskipTests
```

### 5. Start all services

```bash
docker compose up -d
```

### 6. Verify everything is running

```bash
docker compose ps
# All services should show "running" or "healthy"
```

### 7. Access the platform

| Interface | URL |
|---|---|
| 🌐 Frontend | http://localhost:3000 |
| 🔌 API Gateway | http://localhost:8080 |
| 📊 Grafana | http://localhost:3001 |
| 📈 Prometheus | http://localhost:9090 |
| 🗄 MinIO Console | http://localhost:9001 |
| 🤖 ML Service Docs | http://localhost:8088/docs |

---

## Default Credentials

> ⚠️ Change all passwords before any production deployment.

| Service | Username | Password |
|---|---|---|
| PostgreSQL | `seip_user` | `seip_secret_2024` |
| MinIO | `seip_minio_user` | `seip_minio_secret` |
| Grafana | `admin` | `seip_grafana_2024` |
| App Admin | `admin@seip.com` | `admin123` |

---

## Kubernetes Deployment

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/kafka-deployment.yaml
kubectl apply -f k8s/

# Check pod status
kubectl get pods -n seip -w

# View logs
kubectl logs -f deployment/expense-service -n seip

# Scale a service
kubectl scale deployment/fraud-service --replicas=3 -n seip
```

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| API Layer | Spring Cloud Gateway | Routing, auth filtering, rate limiting |
| Services | Spring Boot 3.x | Business logic |
| ML | FastAPI + scikit-learn | Fraud scoring |
| Frontend | React 18 + TypeScript | SPA |
| Database | PostgreSQL 16 | Primary data store |
| Messaging | Apache Kafka | Async events (fraud, audit, notifications) |
| Object Storage | MinIO | Receipt images & documents |
| Search / Logs | Elasticsearch | Log aggregation |
| Metrics | Prometheus + Grafana | Observability |
| Log pipeline | Logstash | Structured log ingestion |
| CI/CD | GitHub Actions | Build, test, push, deploy |
| Container Orchestration | Kubernetes | Production deployment |
| Image Registry | GHCR | Docker image storage |

---

## Architecture Decisions

### ADR-001: API Gateway pattern
All client traffic flows through a single API Gateway (`seip-api-gateway`) which handles JWT validation, request routing, and rate limiting. This keeps auth logic centralised and avoids duplicating it in every service.

### ADR-002: Kafka for async events
Expense submissions emit a Kafka event (`expense.submitted`) consumed by Fraud, Audit, and Notification services independently. This decouples services and allows them to fail without impacting the expense submission flow.

### ADR-003: Phased ML model
Phase 1 uses a heuristic rule-enhanced scorer for immediate fraud detection without training data. Phase 2 upgrades to an Isolation Forest + Random Forest ensemble once sufficient data is collected.

### ADR-004: MinIO for receipt storage
Receipts and documents are stored in MinIO (S3-compatible) rather than the relational database to keep PostgreSQL lean and avoid BLOB-induced performance degradation.

### ADR-005: Single PostgreSQL, schema-per-service
Development uses a single PostgreSQL instance with separate schemas per service for simplicity. Production Kubernetes deployments should use separate databases or a managed RDS instance per service.

---

## Project Structure

```
smart-expense-platform/
├── .github/
│   └── workflows/
│       ├── ci.yml              # Build, test, push Docker images
│       └── cd.yml              # Deploy to Kubernetes
├── services/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── user-service/
│   ├── expense-service/
│   ├── fraud-service/
│   ├── notification-service/
│   ├── audit-service/
│   └── analytics-service/
├── ml-service/                 # Python FastAPI fraud scoring
│   ├── main.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
├── frontend/                   # React + TypeScript SPA
├── k8s/                        # Kubernetes manifests
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── configmap.yaml
│   ├── postgres-deployment.yaml
│   ├── kafka-deployment.yaml
│   ├── auth-deployment.yaml
│   ├── user-deployment.yaml
│   ├── expense-deployment.yaml
│   ├── fraud-deployment.yaml
│   ├── notification-deployment.yaml
│   ├── audit-deployment.yaml
│   ├── analytics-deployment.yaml
│   ├── ml-deployment.yaml
│   ├── gateway-deployment.yaml
│   ├── frontend-deployment.yaml
│   └── ingress.yaml
├── monitoring/
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/prometheus.yml
│   │   │   └── dashboards/dashboard.yml
│   │   └── dashboards/seip-overview.json
│   └── logstash/
│       └── pipeline/logstash.conf
├── db/
│   └── init.sql                # PostgreSQL schema initialisation
├── docker-compose.yml          # Full stack (infra + services + monitoring)
├── .env.example
├── .gitignore
├── mvnw                        # Maven wrapper (Unix)
├── mvnw.cmd                    # Maven wrapper (Windows)
└── README.md
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Write your code and tests
4. Ensure all tests pass: `./mvnw test`
5. Submit a pull request targeting `develop`

### Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Production-ready code, triggers CD pipeline |
| `develop` | Integration branch for features |
| `feature/*` | Individual feature development |
| `hotfix/*` | Urgent production fixes |

---

## License

MIT License — see [LICENSE](LICENSE) for details.
