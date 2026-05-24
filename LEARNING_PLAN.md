# 7-Day Spring Boot Learning Plan, Tied To This Codebase

This plan assumes zero prior Spring Boot exposure. Each day pairs a small reading list with the exact files in this repo that demonstrate the idea. Spend ~3 hours per day; do the reading first, then open the file, then write down (in your own words) why that file is the way it is. That writing step is the one that survives into the interview.

## Day 1 — Core Spring Boot mental model

Goal: understand what `@SpringBootApplication`, IoC/DI, and the auto-configuration story actually mean.

Read:
- Spring Boot reference, "Getting Started" + "Using Spring Boot" chapters (skim, don't memorize)
- Anything on `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`

Open in this repo:
- `KhataLedgerApplication.java` — note the three things `@SpringBootApplication` actually combines
- `application.yml` — see how externalized config works
- `config/SecurityConfig.java` — your first real `@Bean` declarations

Write down (in your notes file): "What does Spring auto-configuration give me that I'd otherwise have to wire by hand?"

## Day 2 — Controllers, DTOs, validation, exception handling

Read:
- `@RestController`, `@RequestMapping`, `@RequestBody`, `@PathVariable`
- Bean Validation (`@Valid`, `@NotBlank`, `@Size`)
- `@ControllerAdvice` + `@ExceptionHandler` + ProblemDetail

Open in this repo:
- `auth/AuthController.java` (start here — smallest controller)
- `customer/CustomerController.java`
- `auth/dto/SignupRequest.java` — Java `record` as a DTO + validation
- `common/GlobalExceptionHandler.java` — centralized error -> JSON mapping

Write: "Why do we never return JPA entities from controllers?"

## Day 3 — JPA and Spring Data

Read:
- `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`
- `JpaRepository<T, ID>` and query-method derivation (`findByPhone`)
- `@Transactional`: when it starts, when it commits, what `readOnly = true` does
- Custom JPQL with `@Query`

Open in this repo:
- `merchant/Merchant.java` and `MerchantRepository.java` (smallest example)
- `customer/Customer.java` — note the deliberate choice to use `Long merchantId` instead of `@ManyToOne Merchant`. Be ready to argue for/against.
- `transaction/TransactionRepository.java` — read the JPQL for `outstandingForCustomer`
- `transaction/TransactionService.java` — read the comment about why we DON'T denormalize balance onto Customer

Write: "When is a derived-from-aggregate value better than a denormalized column, and when isn't it?"

## Day 4 — Security with Spring Security 6 + JWT

Read:
- Spring Security filter chain
- Stateless authentication
- BCrypt password hashing
- JWT structure (header.payload.signature) and HS256

Open in this repo:
- `auth/JwtService.java` — issue() and parse(); note `Keys.hmacShaKeyFor`
- `config/JwtAuthenticationFilter.java` — `OncePerRequestFilter`, why we don't run twice
- `config/SecurityConfig.java` — read every line; the `requestMatchers` whitelist and `addFilterBefore` are interview hotspots
- `auth/AuthService.java` — BCrypt + token issuance flow

Write: "What attack does CSRF protect against, and why is it OK to disable here?"

## Day 5 — Service layer, transactions, business logic

Read:
- Transaction propagation (REQUIRED, REQUIRES_NEW)
- Why `@Transactional` only works on public methods called from outside the bean
- Repository vs Service responsibility split

Open in this repo:
- `transaction/TransactionService.java` — `record()` is the canonical write path
- `ledger/LedgerService.java` — composes multiple repositories, no writes
- `auth/AuthService.java` — mixes writes and reads with `readOnly`

Exercise: trace the path of a single `POST /api/customers/{id}/transactions` request through filter -> controller -> service -> repository -> DB and back.

## Day 6 — Scheduling, async, Strategy pattern, AI integration

Read:
- `@Scheduled` cron format, `@Async` execution
- WebClient for outbound HTTP from a Spring app

Open in this repo:
- `reminder/ReminderService.java` — note how we build `Map<Channel, ReminderChannel>` once in the constructor; this is the Strategy lookup
- `reminder/channel/*` — each implementation is a `@Component`; Spring auto-collects them into the constructor's `List<ReminderChannel>`
- `ai/AskService.java` — the "LLM as router" pattern. Be ready to defend this design over text-to-SQL.
- `ai/GeminiClient.java` — read how we use `WebClient` + JSON mode
- `ai/LocalIntentRouter.java` — deterministic fallback

Write: "Why did we design the AI layer to never let the LLM emit SQL?"

## Day 7 — Tests, observability, ship

Read:
- JUnit 5 vs JUnit 4 (annotations only)
- Mockito basics: `@Mock`, `@InjectMocks`, `when().thenReturn()`
- Spring Boot Actuator (`/actuator/health`, `/actuator/prometheus`)

Open in this repo:
- `test/java/com/khataledger/transaction/TransactionServiceTest.java` — see how we mock repositories and assert on the service in isolation
- `application.yml` — Actuator endpoints exposed
- `Dockerfile` and `docker-compose.yml`

Final exercise: run the project end-to-end (docker compose up), open Swagger, walk through signup -> create customer -> record CREDIT -> dashboard -> ask. Then write the demo script you'd give in the interview, end-to-end, in 90 seconds.

## Interview prep checklist (the LLD round)

Their LLD round (per the 2025 interview experiences) opens with "design patterns you've used and why". You can speak to all three of these from this codebase:

- **Strategy** — `ReminderChannel` and its three implementations, looked up by enum at runtime.
- **Factory** — `Transaction.builder()` (Lombok-generated Builder pattern; explain why we use a builder instead of a constructor with 5+ args).
- **Singleton** — every `@Service` and `@Component` is a singleton-scoped Spring bean by default. Be ready to explain how Spring's `ApplicationContext` is itself a kind of registry.

They will probably ask you to extend the schema (e.g., "add a Product table so each transaction can reference what was sold"). Practice that on paper: which columns, which indexes, which foreign keys, how does it interact with the existing balance query.
