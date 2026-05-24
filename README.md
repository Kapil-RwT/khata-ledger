# Khata Ledger Service

A minimal but production-shaped clone of KhataBook's core flow: merchants record customer udhaar/jama entries, the service computes running balances, sends overdue reminders, and answers natural-language questions about the ledger via an LLM-routed intent layer.

Built as a 1-week learning project to get hands-on with Spring Boot, JPA, Spring Security, and AI integration.

## What's interesting in here (for an interviewer)

- **LLM as router, not interpreter.** The AI endpoint never lets the LLM write SQL. It maps the question to one of a closed set of `QueryIntent` values; the service executes parameterized queries via repositories. Bounded blast radius, testable, safe.
- **Strategy pattern for reminders.** `ReminderChannel` is an interface; SMS / WhatsApp / Email each register as a Spring `@Component`. The dispatcher resolves the right strategy at runtime via a `Map<Channel, ReminderChannel>` built once at startup.
- **Derived balance, not denormalized.** Outstanding balance is a SUM query, not a column on `Customer`. Writes never touch two rows. Defensible in an LLD round.
- **Stateless JWT auth.** No HTTP session, every request carries a bearer token, all controllers receive a `MerchantPrincipal` via `@AuthenticationPrincipal`.
- **Flyway-managed schema.** Hibernate is `ddl-auto=validate`; the database is owned by versioned SQL migrations, not by entity scanning.
- **Graceful AI fallback.** If `GEMINI_API_KEY` is unset, a local keyword router takes over. The project works fully without any external API key.

## Tech

- Backend: Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security, JJWT, Flyway, springdoc-openapi, Lombok
- DB: PostgreSQL 16
- Frontend: React 18 + Vite + Tailwind + Axios + react-router
- Infra: Docker, docker-compose
- Tests: JUnit 5 + Mockito + AssertJ

## Project layout

```
khata-ledger/
├── backend/                   Spring Boot service
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/khataledger/
│       │   ├── KhataLedgerApplication.java
│       │   ├── auth/           signup/login + JWT
│       │   ├── config/         SecurityConfig, JwtAuthFilter, OpenApi
│       │   ├── merchant/       Merchant entity + repo
│       │   ├── customer/       CRUD scoped per merchant
│       │   ├── transaction/    CREDIT/DEBIT entries + balance queries
│       │   ├── ledger/         dashboard summary
│       │   ├── reminder/       scheduled overdue scan + Strategy channels
│       │   ├── ai/             NL question -> intent -> safe query
│       │   └── common/         exceptions + GlobalExceptionHandler
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/V1__init.sql
│       └── test/java/...       sample TransactionService unit tests
└── frontend/                  React app
    └── src/
        ├── pages/             Login, Dashboard, CustomerLedger
        ├── api/client.js      axios with JWT interceptor
        └── store/auth.js
```

## Run it locally

### Prereqs

- Docker + docker-compose
- Java 17, Maven (for running tests outside Docker)
- Node 18+ (for the frontend)

### One-command run (backend + Postgres)

```bash
docker compose up --build
```

Backend boots on `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:5173`. The Vite dev server proxies `/api/*` to `localhost:8080`.

### Optional: enable the LLM path

```bash
export GEMINI_API_KEY=<your key>
docker compose up --build
```

Without it, ledger questions are routed by a local keyword matcher. With it, Gemini classifies the intent. Either way the answer is computed by the same backend repository layer.

## Try it via curl

```bash
# 1. Signup
TOKEN=$(curl -s -X POST localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"businessName":"Ramesh Kirana","phone":"9999999999","password":"hello123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2. Create a customer
curl -s -X POST localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Suresh","phone":"8888888888"}'

# 3. Record udhaar of Rs.500 for customer id 1
curl -s -X POST localhost:8080/api/customers/1/transactions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"type":"CREDIT","amount":500,"note":"samaan"}'

# 4. Ask the ledger
curl -s -X POST localhost:8080/api/ledger/ask \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"question":"Who owes me the most?"}'
```

## Endpoints at a glance

| Method | Path                                       | Auth | Purpose                                          |
|--------|--------------------------------------------|------|--------------------------------------------------|
| POST   | /api/auth/signup                           | -    | Create a merchant, returns JWT                   |
| POST   | /api/auth/login                            | -    | Returns JWT                                      |
| GET    | /api/customers                             | JWT  | List my customers with outstanding               |
| POST   | /api/customers                             | JWT  | Add a customer                                   |
| GET    | /api/customers/{id}                        | JWT  | One customer with outstanding                    |
| DELETE | /api/customers/{id}                        | JWT  | Remove customer + cascade                        |
| POST   | /api/customers/{id}/transactions           | JWT  | Record CREDIT/DEBIT                              |
| GET    | /api/customers/{id}/transactions           | JWT  | Ledger statement (optionally date-filtered)      |
| GET    | /api/ledger/dashboard                      | JWT  | Receivables / payables / overdue summary         |
| POST   | /api/ledger/ask                            | JWT  | NL question -> answer                            |
| GET    | /api/reminders                             | JWT  | List reminders                                   |
| POST   | /api/reminders/scan                        | JWT  | Force-run the overdue scan now                   |

## Resume bullets (after you've built and understood this)

- Built a digital ledger microservice in **Java 17 / Spring Boot 3**, exposing JWT-secured REST APIs for merchants to manage customers and CREDIT/DEBIT transactions with derived running balances computed via **Spring Data JPA** aggregate queries; Flyway-versioned **PostgreSQL** schema and `@Transactional` write paths.
- Designed a **Strategy-pattern reminder dispatcher** with pluggable SMS / WhatsApp / Email channels, driven by a daily `@Scheduled` overdue scan over per-customer activity windows.
- Added a **GenAI ledger-Q&A endpoint** that classifies free-text questions into a closed set of intents via the Gemini API, with a local keyword router as a deterministic fallback — keeping the LLM as a *router*, not a SQL author, so the blast radius is bounded.
- Containerized with multi-stage **Dockerfile** and **docker-compose**, instrumented with Spring **Actuator** for health/metrics, exposed an **OpenAPI/Swagger** spec.

## Citations to the company / interview research that motivated this project

- SDE-I FullStack interview experience at Khatabook (Oct 2025) — confirms Java+SpringBoot focus: https://interviewexperiences.in/experience/khatabook/sde-i-fullstack-1-3-yoe-khatabook-bengaluru
- EngineBogie #728 Khatabook Full Stack Developer interview: https://enginebogie.com/interview/experience/khatabook-full-stack-developer/728
- Khatabook Data Science Intern listing (GenAI/NLP focus): https://jumpwhere.com/explore-exciting-fresher-experienced-roles-at-khatabook/
- VP Engineering on the tech stack (Technuter): https://technuter.com/breaking-news/interview-with-gaurav-lahoti-vp-engineering-at-khatabook.html
