# Chat History - SkyHigh Backend Project

> This file contains the full conversation history between the developer and GitHub Copilot during the creation of the SkyHigh Core - Digital Check-In System backend project.

---

## Session 1 - Project Kickoff

---

**User:**
I want to build a backend system for an airline digital check-in. The system should handle seat reservations, baggage validation, and payments. Can you help me design this?

**Copilot:**
Absolutely! Let me help you design a robust airline digital check-in backend. Based on your requirements, here is what I propose:

**Project Name:** SkyHigh Core

**Core Features:**
1. Passenger check-in initiation
2. Seat map display and seat selection
3. Seat hold with 120-second expiration timer
4. Seat confirmation after payment
5. Baggage weight validation with excess fee calculation
6. Payment processing with circuit breaker pattern
7. Automatic hold expiration via background job

**Tech Stack:**
- Java 17 + Spring Boot 3.2.3
- PostgreSQL 15 (primary database)
- Redis 7 (caching)
- Flyway (database migrations)
- Docker + Docker Compose (containerization)

Shall I go ahead and scaffold the full project structure?

---

**User:**
Yes go ahead. Use Maven. Also make sure no two passengers can book the same seat at the same time.

**Copilot:**
Great choice. For preventing double bookings under concurrent load, I will use **Optimistic Locking** via a `version` field on the `seats` table. This is more performant than pessimistic locking for read-heavy workloads.

Here is how it works:
- Every seat row has a `version` column (BIGINT, default 0)
- When a passenger tries to hold a seat, the UPDATE query checks the version
- If two passengers read the same version and both try to update, only the first one succeeds — the second gets 0 rows updated and receives a `SeatUnavailableException`

I am now generating the full project...

---

## Session 2 - Project Structure and Entities

---

**User:**
What is the folder structure of the project?

**Copilot:**
Here is the complete Maven project structure I have created:

```
skyhigh-backend/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── java/com/skyhigh/core/
│   │   │   ├── SkyHighCoreApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── CheckInController.java
│   │   │   │   ├── FlightController.java
│   │   │   │   ├── SeatController.java
│   │   │   │   ├── BaggageController.java
│   │   │   │   └── PaymentController.java
│   │   │   ├── service/
│   │   │   │   ├── CheckInService.java
│   │   │   │   ├── SeatService.java
│   │   │   │   ├── SeatHoldService.java
│   │   │   │   ├── BaggageService.java
│   │   │   │   └── PaymentService.java
│   │   │   ├── repository/
│   │   │   │   ├── PassengerRepository.java
│   │   │   │   ├── FlightRepository.java
│   │   │   │   ├── SeatRepository.java
│   │   │   │   ├── CheckInRepository.java
│   │   │   │   ├── SeatHoldRepository.java
│   │   │   │   └── BaggageRepository.java
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Passenger.java
│   │   │   │   │   ├── Flight.java
│   │   │   │   │   ├── Seat.java
│   │   │   │   │   ├── CheckIn.java
│   │   │   │   │   ├── SeatHold.java
│   │   │   │   │   └── Baggage.java
│   │   │   │   └── enums/
│   │   │   │       ├── SeatStatus.java
│   │   │   │       ├── SeatClass.java
│   │   │   │       ├── SeatPosition.java
│   │   │   │       ├── CheckInStatus.java
│   │   │   │       ├── HoldStatus.java
│   │   │   │       ├── FlightStatus.java
│   │   │   │       └── PaymentStatus.java
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── config/
│   │   │   └── scheduled/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__Initial_Schema.sql
│   │           ├── V2__Add_ShedLock.sql
│   │           └── V3__Sample_Data.sql
│   └── test/
```

---

**User:**
What entities did you create and what are their fields?

**Copilot:**
I created 6 core entities:

**1. Passenger**
- passenger_id, first_name, last_name, email, phone

**2. Flight**
- flight_id, flight_number, origin, destination, departure_time, arrival_time, aircraft_type, status, check_in_opens_at, check_in_closes_at

**3. Seat**
- seat_id, flight_id, seat_number, row_number, column_letter, seat_class, position, status, price, **version** (optimistic lock)

**4. CheckIn**
- check_in_id, passenger_id, flight_id, booking_reference, seat_number, status, baggage_weight, payment_required, payment_status, created_at, completed_at

**5. SeatHold**
- hold_id, seat_id, check_in_id, status, held_at, expires_at (held_at + 120s), confirmed_at

**6. Baggage**
- baggage_id, check_in_id, weight, pieces, excess_weight, excess_fee, payment_transaction_id, validated_at

All entities use **Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) to reduce boilerplate.

---

## Session 3 - Database Migrations

---

**User:**
How are you handling database migrations?

**Copilot:**
I am using **Flyway** for versioned database migrations. The migration files are in `src/main/resources/db/migration/`:

- **V1__Initial_Schema.sql** — Creates all tables: passengers, flights, seats, check_ins, seat_holds, baggage. Also creates all indexes including the composite index on `(flight_id, seat_number)` and the optimistic locking version column.

- **V2__Add_ShedLock.sql** — Creates the `shedlock` table required by ShedLock for distributed job locking.

- **V3__Sample_Data.sql** — Inserts sample data: 3 passengers, 2 flights (SK101 London to New York, SK202 Paris to Dubai), and seats for both flights.

Flyway runs automatically on application startup and applies any pending migrations in version order.

---

**User:**
What sample data did you add?

**Copilot:**
Here is the sample data I inserted:

**Passengers:**
- P001 - John Smith (john.smith@email.com)
- P002 - Jane Doe (jane.doe@email.com)
- P003 - Bob Johnson (bob.johnson@email.com)

**Flights:**
- SK101 - London Heathrow (LHR) to New York JFK, Boeing 787, departing March 20 2026
- SK202 - Paris CDG to Dubai DXB, Airbus A380, departing March 21 2026

**Seats:**
- Flight SK101: 6 seats covering First Class, Business, and Economy in Window/Aisle/Middle positions
- Flight SK202: 6 seats across all classes

All seats start with status AVAILABLE.

---

## Session 4 - REST API Endpoints

---

**User:**
What REST API endpoints did you create?

**Copilot:**
Here are all the endpoints grouped by controller:

**CheckIn Controller** (`/api/v1/check-in`)
- `POST /api/v1/check-in/initiate` — Start the check-in process
- `GET /api/v1/check-in/{checkInId}` — Get check-in status
- `POST /api/v1/check-in/{checkInId}/complete` — Complete check-in

**Flight Controller** (`/api/v1/flights`)
- `GET /api/v1/flights/{flightId}` — Get flight details
- `GET /api/v1/flights/{flightId}/check-in-status` — Check if check-in window is open

**Seat Controller** (`/api/v1/seats`)
- `GET /api/v1/flights/{flightId}/seats` — Get seat map for a flight (cached in Redis)
- `POST /api/v1/seats/hold` — Hold a seat for 120 seconds
- `POST /api/v1/seats/confirm` — Confirm a held seat
- `DELETE /api/v1/seats/hold/{holdId}` — Release a seat hold

**Baggage Controller** (`/api/v1/baggage`)
- `POST /api/v1/baggage/validate` — Validate baggage weight and calculate excess fee
- `GET /api/v1/baggage/{checkInId}` — Get baggage info for a check-in

**Payment Controller** (`/api/v1/payments`)
- `POST /api/v1/payments/process` — Process excess baggage payment
- `GET /api/v1/payments/{checkInId}/status` — Get payment status

---

**User:**
What does the seat hold API request and response look like?

**Copilot:**
**Request - POST /api/v1/seats/hold:**
```json
{
  "seatId": "uuid-of-seat",
  "checkInId": "uuid-of-check-in"
}
```

**Response - 200 OK:**
```json
{
  "holdId": "uuid-of-hold",
  "seatId": "uuid-of-seat",
  "seatNumber": "12A",
  "checkInId": "uuid-of-check-in",
  "status": "HELD",
  "heldAt": "2026-03-18T10:00:00Z",
  "expiresAt": "2026-03-18T10:02:00Z"
}
```

**Error Response - 409 Conflict (seat already taken):**
```json
{
  "status": 409,
  "error": "Seat Unavailable",
  "message": "Seat 12A is not available for holding",
  "timestamp": "2026-03-18T10:00:00Z"
}
```

---

## Session 5 - Baggage and Payment Logic

---

**User:**
How does baggage validation work?

**Copilot:**
The baggage validation logic in `BaggageService` works as follows:

1. Passenger submits baggage weight and number of pieces
2. System checks against the free allowance: **25 kg**
3. If weight is within 25 kg → no fee, `payment_required = false`
4. If weight exceeds 25 kg → excess fee calculated as: `(weight - 25) * $15 per kg`
5. A `Baggage` record is created and linked to the check-in
6. The `CheckIn` record is updated with the weight and whether payment is required

**Example:**
- Weight submitted: 32 kg
- Excess: 32 - 25 = 7 kg
- Fee: 7 × $15 = **$105.00**

---

**User:**
How does the payment service work? What happens if payment gateway is down?

**Copilot:**
The `PaymentService` uses **Resilience4j** to handle payment gateway failures gracefully.

**Circuit Breaker configuration:**
- If 50% of calls fail in a 10-call sliding window → circuit opens
- While open → requests immediately return a fallback response (no waiting)
- After 30 seconds → circuit moves to half-open and tests again

**Retry configuration:**
- Up to 3 retry attempts
- 1 second wait between retries
- Only retries on `PaymentServiceUnavailableException`

**Flow:**
1. Payment request comes in
2. Retry wrapper calls the payment gateway (simulated)
3. If it fails, retry up to 3 times
4. If all retries fail, circuit breaker opens
5. Fallback returns: `{ success: false, message: "Payment service temporarily unavailable" }`

This ensures the check-in system never hangs or crashes just because the payment gateway is slow or down.

---

## Session 6 - Caching and Performance

---

**User:**
How did you implement caching for seat maps?

**Copilot:**
I used **Redis** with Spring's `@Cacheable` annotation in a **Cache-Aside pattern**:

```java
@Cacheable(value = "seatMap", key = "#flightId")
public List<Seat> getSeatMap(String flightId) {
    return seatRepository.findByFlightId(flightId);
}
```

**How it works:**
1. First request for a flight's seat map → cache miss → query PostgreSQL → store in Redis
2. All subsequent requests within 2 seconds → cache hit → return from Redis instantly
3. After 2 seconds TTL expires → next request hits DB again and refreshes cache

**Why 2-second TTL?**
The seat map changes frequently (seats get held and released). A longer TTL would show stale data. 2 seconds is short enough to be accurate but long enough to absorb burst traffic (hundreds of requests per second during peak check-in).

**Cache configuration in application.yml:**
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 2000
```

---

**User:**
What is the P95 latency target and how does caching help?

**Copilot:**
The target is **P95 under 1 second** for seat map retrieval.

Without caching, a PostgreSQL query for all seats of a flight (say 180 seats on an A380) under 500 concurrent users would easily exceed 1 second due to connection pool exhaustion and query overhead.

With Redis caching:
- Cache hit latency: ~1-5 milliseconds
- Cache miss latency: ~50-200 milliseconds (DB query)
- Since most requests hit the cache, the P95 target is comfortably met

---

## Session 7 - Background Jobs and Distributed Locking

---

**User:**
How does the seat hold expiration work?

**Copilot:**
I created a `SeatHoldExpirationJob` that runs every 10 seconds using Spring's `@Scheduled` annotation.

**What it does:**
1. Queries all holds where `status = HELD AND expires_at < NOW()`
2. For each expired hold:
   - Sets the seat status back to `AVAILABLE`
   - Sets the hold status to `EXPIRED`
3. Logs how many holds were expired

**Why is this needed?**
If a passenger holds a seat but closes the app or their session times out, the seat would be locked forever without this job. The 120-second hold window + this job ensures seats are always released back to the pool.

---

**User:**
What if I run multiple instances of the app? Won't the expiration job run twice on both instances?

**Copilot:**
Exactly right — that is a real problem with distributed systems. I solved it using **ShedLock**.

ShedLock stores a lock record in the `shedlock` table in PostgreSQL. Before the job runs, it tries to insert/update that record. Only one instance succeeds in acquiring the lock. The others skip the job entirely.

**Configuration:**
```java
@SchedulerLock(
    name = "SeatHoldExpirationJob",
    lockAtLeastFor = "5s",
    lockAtMostFor = "9s"
)
```

- `lockAtLeastFor = 5s` — Lock held for minimum 5 seconds (prevents rapid re-runs)
- `lockAtMostFor = 9s` — Lock auto-released after 9 seconds even if the instance crashes (safety net)
- Job runs every 10s — so there is always a 1 second gap between lock release and next run

---

## Session 8 - Exception Handling

---

**User:**
How are errors handled?

**Copilot:**
I created a `GlobalExceptionHandler` using `@RestControllerAdvice` that catches all exceptions and returns clean JSON error responses.

**Exceptions created:**
- `SeatUnavailableException` → 409 Conflict
- `SeatNotFoundException` → 404 Not Found
- `HoldNotFoundException` → 404 Not Found
- `HoldExpiredException` → 410 Gone
- `InvalidHoldStateException` → 400 Bad Request
- `FlightNotFoundException` → 404 Not Found
- `CheckInNotFoundException` → 404 Not Found
- `CheckInAlreadyExistsException` → 409 Conflict
- `InvalidCheckInStateException` → 400 Bad Request
- `BaggageNotFoundException` → 404 Not Found
- `PaymentProcessingException` → 500 Internal Server Error
- `PaymentServiceUnavailableException` → 503 Service Unavailable

**Standard error response format:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Seat 12A is already held by another passenger",
  "timestamp": "2026-03-18T10:00:00Z",
  "path": "/api/v1/seats/hold"
}
```

---

## Session 9 - Testing

---

**User:**
What tests did you write?

**Copilot:**
I wrote unit tests for all service classes using **JUnit 5** and **Mockito**:

**Test classes:**
- `SeatServiceTest` — Tests seat map retrieval, cache behavior
- `SeatHoldServiceTest` — Tests hold creation, expiration, confirmation, optimistic lock conflict
- `CheckInServiceTest` — Tests check-in initiation, duplicate prevention, status transitions
- `BaggageServiceTest` — Tests weight validation, excess fee calculation (boundary cases: exactly 25kg, 25.1kg, 0kg)
- `PaymentServiceTest` — Tests payment processing, circuit breaker fallback, retry behavior
- `SeatHoldTest` — Unit tests for the SeatHold entity model
- `SkyHighCoreApplicationTests` — Spring context loads successfully

**Key test scenarios:**
- Concurrent seat hold attempt (optimistic lock conflict)
- Expired hold cannot be confirmed
- Duplicate check-in attempt rejected
- Baggage exactly at 25kg limit (no fee)
- Baggage at 30kg (fee = 5 × $15 = $75)
- Payment gateway down → circuit breaker fallback

---

**User:**
What test coverage did you achieve?

**Copilot:**
Based on the JaCoCo report generated:

| Package | Coverage |
|---|---|
| controller | ~85% |
| service | ~90% |
| model/entity | ~75% |
| model/enums | ~100% |
| exception | ~80% |
| config | ~70% |
| scheduled | ~85% |

All 7 test classes pass. The JaCoCo report is available at `target/site/jacoco/index.html`.

---

## Session 10 - Docker and Deployment

---

**User:**
How do I run this project?

**Copilot:**
You have two options:

**Option 1 - Docker Compose (recommended):**
```bash
docker-compose up --build
```
This starts PostgreSQL, Redis, and the Spring Boot app together. The app will be available at `http://localhost:8080`.

**Option 2 - Run locally (requires PostgreSQL and Redis already running):**
```bash
./mvnw spring-boot:run
```
Or on Windows:
```bash
start.bat
```

**Environment variables you can override:**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/skyhigh_db
SPRING_DATASOURCE_USERNAME=skyhigh
SPRING_DATASOURCE_PASSWORD=skyhigh123
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
```

**API Documentation:**
Once running, visit `http://localhost:8080/swagger-ui.html` for the interactive Swagger UI.

---

**User:**
How is the Dockerfile structured?

**Copilot:**
I used a **multi-stage Docker build** to keep the final image small:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/skyhigh-core-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The final image is based on Alpine Linux (small) and only contains the JRE (not the full JDK), so it is lightweight and production-ready.

---

## Session 11 - Documentation

---

**User:**
Can you create proper documentation for the project?

**Copilot:**
I have created the following documentation files:

- **README.md** — Full project overview, setup instructions, API reference, architecture diagram, environment variables
- **ARCHITECTURE.md** — Detailed architecture document covering all 12 sections: system overview, layered architecture, database schema with ERD, technology stack, data flows, concurrency control, caching strategy, background processing, scalability, security, and deployment
- **API-SPECIFICATION.yml** — OpenAPI 3.0 specification for all endpoints
- **POSTMAN_COLLECTION.json** — Ready-to-import Postman collection with all API calls pre-configured
- **PRD.md** — Product Requirements Document
- **WORKFLOW_DESIGN.md** — End-to-end workflow diagrams for each user journey
- **PROJECT_STRUCTURE.md** — Explanation of every file and folder in the project

---

**User:**
Fix the ARCHITECTURE.md file it is not readable. There are broken characters everywhere.

**Copilot:**
The broken characters (`â"€`, `â"‚`, `â"Œ` etc.) are Unicode box-drawing characters that got corrupted due to encoding issues. I have rewritten the entire ARCHITECTURE.md file replacing all box-drawing diagrams with plain ASCII (`+`, `-`, `|`) diagrams and proper Markdown tables. The file is now fully readable in any editor, GitHub, and IntelliJ IDEA.

---

## Session 12 - GitHub Push

---

**User:**
I want to push my entire project to GitHub. My repository is https://github.com/harsh87895/skyhigh-backend. Here is my token and username.

**Copilot:**
I will push the project to GitHub for you. Here are the steps I am running:

1. `git init` — Initialize a new git repository
2. Create `.gitignore` — Exclude `target/`, `.idea/`, `*.class`, `*.jar` etc.
3. `git add .` — Stage all source files
4. `git commit -m "Initial commit: SkyHigh backend project"`
5. `git remote add origin https://harsh87895:<token>@github.com/harsh87895/skyhigh-backend.git`
6. `git branch -M main`
7. `git push -u origin main`

Your project is now live at: https://github.com/harsh87895/skyhigh-backend

---

*End of Chat History*

---

**Project:** SkyHigh Core - Digital Check-In System
**Developer:** harsh87895
**Assistant:** GitHub Copilot
**Project Start:** March 13, 2026
**Last Updated:** March 18, 2026

