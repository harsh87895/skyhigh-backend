# Project Structure

## Overview

This document explains the structure of the SkyHigh Core project, detailing the purpose of each folder and key modules.

---

## Directory Structure

```
skyhigh-backend/
├── src/
│   ├── main/
│   │   ├── java/com/skyhigh/core/
│   │   │   ├── SkyHighCoreApplication.java
│   │   │   ├── client/
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java
│   │   │   │   └── SchedulingConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── BaggageController.java
│   │   │   │   ├── CheckInController.java
│   │   │   │   ├── FlightController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   └── SeatController.java
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Baggage.java
│   │   │   │   │   ├── CheckIn.java
│   │   │   │   │   ├── Flight.java
│   │   │   │   │   ├── Passenger.java
│   │   │   │   │   ├── Seat.java
│   │   │   │   │   └── SeatHold.java
│   │   │   │   └── enums/
│   │   │   │       ├── CheckInStatus.java
│   │   │   │       ├── FlightStatus.java
│   │   │   │       ├── HoldStatus.java
│   │   │   │       ├── PaymentStatus.java
│   │   │   │       ├── SeatClass.java
│   │   │   │       ├── SeatPosition.java
│   │   │   │       └── SeatStatus.java
│   │   │   ├── repository/
│   │   │   │   ├── BaggageRepository.java
│   │   │   │   ├── CheckInRepository.java
│   │   │   │   ├── FlightRepository.java
│   │   │   │   ├── PassengerRepository.java
│   │   │   │   ├── SeatHoldRepository.java
│   │   │   │   └── SeatRepository.java
│   │   │   ├── scheduled/
│   │   │   │   └── SeatHoldExpirationJob.java
│   │   │   └── service/
│   │   │       ├── BaggageService.java
│   │   │       ├── CheckInService.java
│   │   │       ├── PaymentService.java
│   │   │       ├── SeatHoldService.java
│   │   │       └── SeatService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__Initial_Schema.sql
│   │           ├── V2__Add_ShedLock.sql
│   │           └── V3__Sample_Data.sql
│   └── test/
│       ├── java/com/skyhigh/core/
│       │   ├── model/
│       │   │   └── SeatHoldTest.java
│       │   ├── service/
│       │   │   ├── BaggageServiceTest.java
│       │   │   ├── CheckInServiceTest.java
│       │   │   ├── SeatHoldServiceTest.java
│       │   │   └── SeatServiceTest.java
│       │   └── SkyHighCoreApplicationTests.java
│       └── resources/
│           └── application-test.yml
├── docs/
│   ├── PRD.md
│   └── ARCHITECTURE.md
├── target/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── PRD.md
├── README.md
├── ARCHITECTURE.md
├── PROJECT_STRUCTURE.md
├── WORKFLOW_DESIGN.md
├── API-SPECIFICATION.yml
└── CHAT_HISTORY.md
```

---

## Root Directory Files

### `pom.xml`
**Purpose:** Maven project configuration file.

**Key Dependencies:**
- Spring Boot 3.2.3 (Web, Data JPA, Data Redis, Validation, Actuator)
- PostgreSQL Driver
- Flyway (database migrations)
- Lombok (code generation)
- Resilience4j (circuit breaker, retry)
- ShedLock (distributed locking for scheduled jobs)
- Swagger/OpenAPI (API documentation)

**Build Configuration:**
- Java 17 compilation target
- Spring Boot Maven Plugin for packaging
- JaCoCo for test coverage

---

### `docker-compose.yml`
**Purpose:** Multi-container Docker application setup.

**Services:**
1. **postgres** - PostgreSQL 15 database
   - Port: 5432
   - Volume: `postgres_data` for persistence
   - Health check enabled

2. **redis** - Redis 7 cache
   - Port: 6379
   - Volume: `redis_data` for persistence
   - Append-only file enabled

3. **app** - Spring Boot application
   - Port: 8080
   - Depends on postgres and redis health
   - Environment variables for configuration

---

### `Dockerfile`
**Purpose:** Containerization instructions for the Spring Boot application.

**Multi-stage build:**
1. **Build stage:** Maven build to create JAR
2. **Runtime stage:** Minimal JRE 17 image with application JAR

---

### `README.md`
**Purpose:** Project overview and setup instructions.

**Contents:**
- Quick start guide
- Docker and local setup instructions
- API endpoint documentation
- Troubleshooting guide

---

### `PROJECT_STRUCTURE.md` (this file)
**Purpose:** Explains the project structure and module purposes.

---

### `WORKFLOW_DESIGN.md`
**Purpose:** Documents implementation workflows and flow diagrams.

**Contents:**
- Check-in flow
- Seat hold/confirm flow
- Baggage validation flow
- Background job flow

---

### `API-SPECIFICATION.yml`
**Purpose:** OpenAPI 3.0 specification for all REST endpoints.

---

### `CHAT_HISTORY.md`
**Purpose:** Chronicles the design journey and key decisions made with AI assistance.

---

## Source Code Structure (`src/main/java/com/skyhigh/core/`)

### `SkyHighCoreApplication.java`
**Purpose:** Main Spring Boot application entry point.

**Annotations:**
- `@SpringBootApplication` - Auto-configuration
- `@EnableCaching` - Redis caching
- `@EnableScheduling` - Background jobs
- `@EnableJpaRepositories` - JPA repositories

---

### `client/`
**Purpose:** External service clients (e.g., payment gateway).

**Key Classes:**
- Future: Real payment gateway integration
- Future: Weight validation service client

**Pattern:** Resilience4j circuit breaker and retry

---

### `config/`
**Purpose:** Spring configuration classes.

#### `CacheConfig.java`
- Redis cache configuration
- Cache manager setup
- TTL configuration for seat maps (2 seconds)

#### `SchedulingConfig.java`
- ShedLock configuration for distributed locking
- Ensures only one instance runs scheduled jobs
- Lock provider: JDBC-based (PostgreSQL)

#### Future: `SecurityConfig.java`
- JWT authentication (out of scope for v1)

---

### `controller/`
**Purpose:** REST API controllers (presentation layer).

#### `CheckInController.java`
**Endpoints:**
- `POST /api/v1/check-in/start` - Start check-in
- `GET /api/v1/check-in/{checkInId}` - Get check-in details
- `PUT /api/v1/check-in/{checkInId}/complete` - Complete check-in

**Responsibilities:**
- Request validation
- Delegate to `CheckInService`
- Map responses to DTOs

---

#### `FlightController.java`
**Endpoints:**
- `GET /api/v1/flights/{flightId}/seats` - Get seat map (cached)

**Responsibilities:**
- Seat map retrieval
- Query parameter filtering (seat class, position)
- Cache hit/miss logging

---

#### `SeatController.java`
**Endpoints:**
- `POST /api/v1/seats/hold` - Hold a seat for 120 seconds
- `POST /api/v1/seats/confirm` - Confirm a held seat

**Responsibilities:**
- Seat reservation orchestration
- Delegate to `SeatHoldService`
- Handle concurrency errors

---

#### `BaggageController.java`
**Endpoints:**
- `POST /api/v1/baggage/validate` - Validate baggage weight

**Responsibilities:**
- Baggage weight validation
- Fee calculation
- Payment requirement detection

---

#### `PaymentController.java`
**Endpoints:**
- `POST /api/v1/payment/process` - Process payment (MOCK)

**Responsibilities:**
- Mock payment processing (always succeeds)
- Future: Real payment gateway integration

---

### `dto/`
**Purpose:** Data Transfer Objects for API requests/responses.

**Request DTOs:**
- `CheckInRequest` - Start check-in
- `HoldSeatRequest` - Hold a seat
- `ConfirmSeatRequest` - Confirm held seat
- `BaggageRequest` - Validate baggage
- `PaymentRequest` - Process payment

**Response DTOs:**
- `CheckInResponse` - Check-in details
- `HoldSeatResponse` - Hold confirmation
- `ConfirmSeatResponse` - Confirmation details
- `BaggageResponse` - Baggage validation result
- `PaymentResponse` - Payment result
- `SeatMapResponse` - Seat availability map
- `ErrorResponse` - Error details

**Pattern:** 
- Lombok `@Builder` for immutability
- Jakarta validation annotations (`@NotNull`, `@Positive`, etc.)

---

### `exception/`
**Purpose:** Custom exception classes and global exception handler.

**Custom Exceptions:**
- `SeatNotFoundException` - Seat does not exist
- `SeatUnavailableException` - Seat already held/confirmed
- `FlightNotFoundException` - Flight does not exist
- `CheckInNotFoundException` - Check-in does not exist
- `CheckInAlreadyExistsException` - Duplicate check-in attempt
- `BaggageNotFoundException` - Baggage record not found
- `PaymentServiceUnavailableException` - Payment service down

#### `GlobalExceptionHandler.java`
**Purpose:** Centralized exception handling.

**Mappings:**
- `SeatUnavailableException` → 409 CONFLICT
- `SeatNotFoundException` → 404 NOT FOUND
- `OptimisticLockException` → 409 CONFLICT (seat already modified)
- `MethodArgumentNotValidException` → 400 BAD REQUEST (validation errors)
- Generic exceptions → 500 INTERNAL SERVER ERROR

---

### `model/`
**Purpose:** Domain entities and enums.

#### `model/entity/`

##### `Passenger.java`
**Table:** `passengers`

**Fields:**
- `passengerId` (PK)
- `firstName`, `lastName`
- `email`, `phone`

---

##### `Flight.java`
**Table:** `flights`

**Fields:**
- `flightId` (PK)
- `flightNumber`, `origin`, `destination`
- `departureTime`, `arrivalTime`
- `status` (enum: SCHEDULED, BOARDING, DEPARTED, CANCELLED)
- `checkInOpensAt`, `checkInClosesAt`

---

##### `Seat.java`
**Table:** `seats`

**Fields:**
- `seatId` (UUID, PK)
- `flightId` (FK)
- `seatNumber` (e.g., "12A")
- `rowNumber`, `columnLetter`
- `seatClass` (enum: ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST)
- `position` (enum: WINDOW, MIDDLE, AISLE)
- `status` (enum: AVAILABLE, HELD, CONFIRMED)
- `price`
- **`version`** (optimistic locking)

**Concurrency Control:**
- `@Version` annotation enables optimistic locking
- Prevents concurrent modifications
- Throws `OptimisticLockException` on conflict

---

##### `CheckIn.java`
**Table:** `check_ins`

**Fields:**
- `checkInId` (UUID, PK)
- `passengerId` (FK)
- `flightId` (FK)
- `bookingReference`
- `seatNumber`
- `status` (enum: INITIATED, AWAITING_PAYMENT, SEAT_SELECTED, COMPLETED)
- `baggageWeight`, `paymentRequired`, `paymentStatus`
- `createdAt`, `completedAt`

**Unique Constraint:** One check-in per passenger per flight

---

##### `SeatHold.java`
**Table:** `seat_holds`

**Fields:**
- `holdId` (UUID, PK)
- `seatId` (FK)
- `checkInId` (FK)
- `status` (enum: HELD, CONFIRMED, EXPIRED)
- `heldAt`, `expiresAt`, `confirmedAt`

**Business Logic:**
- `expiresAt = heldAt + 120 seconds`
- Background job expires holds

---

##### `Baggage.java`
**Table:** `baggage`

**Fields:**
- `baggageId` (UUID, PK)
- `checkInId` (FK)
- `weight`, `pieces`
- `excessWeight`, `excessFee`
- `paymentTransactionId`
- `validatedAt`

**Calculation:**
- If `weight > 25kg`: `excessWeight = weight - 25`, `excessFee = excessWeight * 10`

---

#### `model/enums/`

##### `SeatStatus.java`
- AVAILABLE
- HELD
- CONFIRMED

##### `SeatClass.java`
- ECONOMY
- PREMIUM_ECONOMY
- BUSINESS
- FIRST

##### `SeatPosition.java`
- WINDOW
- MIDDLE
- AISLE

##### `CheckInStatus.java`
- INITIATED
- AWAITING_PAYMENT
- SEAT_SELECTED
- COMPLETED

##### `FlightStatus.java`
- SCHEDULED
- BOARDING
- DEPARTED
- CANCELLED

---

### `repository/`
**Purpose:** Data access layer (Spring Data JPA repositories).

#### `PassengerRepository.java`
```java
public interface PassengerRepository extends JpaRepository<Passenger, String> {
    Optional<Passenger> findByEmail(String email);
}
```

---

#### `FlightRepository.java`
```java
public interface FlightRepository extends JpaRepository<Flight, String> {
    List<Flight> findByStatus(FlightStatus status);
}
```

---

#### `SeatRepository.java`
```java
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByFlightId(String flightId);
    Optional<Seat> findByFlightIdAndSeatNumber(String flightId, String seatNumber);
    
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<Seat> findBySeatIdAndStatus(UUID seatId, SeatStatus status);
}
```

**Optimistic Locking:** `@Lock` annotation ensures version increment

---

#### `CheckInRepository.java`
```java
public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    Optional<CheckIn> findByPassengerIdAndFlightId(String passengerId, String flightId);
    List<CheckIn> findByStatus(CheckInStatus status);
}
```

---

#### `SeatHoldRepository.java`
```java
public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {
    Optional<SeatHold> findBySeatIdAndStatus(UUID seatId, HoldStatus status);
    
    @Query("SELECT sh FROM SeatHold sh WHERE sh.status = 'HELD' AND sh.expiresAt < :currentTime")
    List<SeatHold> findExpiredHolds(@Param("currentTime") Instant currentTime);
}
```

**Custom Query:** Finds expired holds for background job

---

#### `BaggageRepository.java`
```java
public interface BaggageRepository extends JpaRepository<Baggage, UUID> {
    Optional<Baggage> findByCheckInId(UUID checkInId);
}
```

---

### `service/`
**Purpose:** Business logic layer.

#### `CheckInService.java`
**Responsibilities:**
- Orchestrate check-in flow
- Validate passenger and flight existence
- Prevent duplicate check-ins
- Update check-in status

**Key Methods:**
- `startCheckIn(CheckInRequest)` - Create new check-in
- `getCheckIn(UUID)` - Retrieve check-in details
- `completeCheckIn(UUID)` - Finalize check-in

---

#### `SeatService.java`
**Responsibilities:**
- Seat map retrieval with caching
- Filter seats by class and position
- Cache management (2-second TTL)

**Key Methods:**
- `getSeatMap(flightId, seatClass, position)` - Get seat availability (cached)

**Caching:**
```java
@Cacheable(value = "seatMap", key = "#flightId")
public List<Seat> getSeatMap(String flightId) { ... }
```

---

#### `SeatHoldService.java`
**Responsibilities:**
- Hold seat for 120 seconds
- Confirm held seat
- Optimistic locking handling

**Key Methods:**
- `holdSeat(HoldSeatRequest)` - Create hold with expiration
- `confirmSeat(ConfirmSeatRequest)` - Permanent assignment

**Concurrency Control:**
```java
@Transactional
public SeatHold holdSeat(UUID seatId, UUID checkInId) {
    Seat seat = seatRepository.findBySeatIdAndStatus(seatId, AVAILABLE)
        .orElseThrow(() -> new SeatUnavailableException(...));
    
    seat.setStatus(HELD);
    seat.setVersion(seat.getVersion() + 1); // Optimistic lock
    seatRepository.save(seat);
    
    // Create hold record
    SeatHold hold = new SeatHold();
    hold.setExpiresAt(Instant.now().plusSeconds(120));
    return seatHoldRepository.save(hold);
}
```

---

#### `BaggageService.java`
**Responsibilities:**
- Validate baggage weight
- Calculate excess fees
- Update check-in payment requirement

**Key Methods:**
- `validateBaggage(BaggageRequest)` - Calculate fees and create baggage record

**Fee Calculation:**
```java
if (weight > MAX_FREE_WEIGHT) {
    excessWeight = weight - MAX_FREE_WEIGHT;
    excessFee = excessWeight * FEE_PER_KG;
    checkIn.setPaymentRequired(true);
}
```

---

#### `PaymentService.java`
**Responsibilities:**
- Mock payment processing (always succeeds)
- Future: Real payment gateway integration

**Key Methods:**
- `processPayment(PaymentRequest)` - Mock payment (returns success)

**Circuit Breaker:**
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
@Retry(name = "paymentService")
public PaymentResponse processPayment(PaymentRequest request) {
    // Future: Call real payment gateway
    return PaymentResponse.success(...);
}
```

---

### `scheduled/`
**Purpose:** Background jobs and scheduled tasks.

#### `SeatHoldExpirationJob.java`
**Purpose:** Automatically expire seat holds after 120 seconds.

**Schedule:** Every 10 seconds  
**Distributed Lock:** ShedLock ensures single execution across instances

```java
@Scheduled(fixedRateString = "${app.seat-hold.expiration-job-rate}")
@SchedulerLock(
    name = "SeatHoldExpirationJob",
    lockAtLeastFor = "5s",
    lockAtMostFor = "9s"
)
public void expireHolds() {
    List<SeatHold> expiredHolds = seatHoldRepository.findExpiredHolds(Instant.now());
    
    for (SeatHold hold : expiredHolds) {
        Seat seat = seatRepository.findById(hold.getSeatId()).orElseThrow();
        seat.setStatus(AVAILABLE);
        seatRepository.save(seat);
        
        hold.setStatus(EXPIRED);
        seatHoldRepository.save(hold);
        
        log.info("Expired hold {} for seat {}", hold.getHoldId(), seat.getSeatNumber());
    }
}
```

---

## Resources (`src/main/resources/`)

### `application.yml`
**Purpose:** Application configuration.

**Sections:**
- Database connection (PostgreSQL)
- Redis cache configuration
- Flyway migration settings
- Logging levels
- Custom properties (seat hold duration, baggage fees)

---

### `db/migration/`
**Purpose:** Flyway database migration scripts.

#### `V1__Initial_Schema.sql`
Creates all tables:
- passengers, flights, seats, check_ins, seat_holds, baggage
- Indexes for performance
- Foreign key constraints

#### `V2__Add_ShedLock.sql`
Creates `shedlock` table for distributed job locking.

#### `V3__Sample_Data.sql`
Inserts sample data for testing:
- 5 passengers
- 2 flights
- 180 seats per flight
- Various seat classes and positions

---

## Test Structure (`src/test/java/com/skyhigh/core/`)

### `controller/` (Unit Tests)
- Test REST endpoints in isolation
- Mock service layer
- Validate request/response handling

### `service/` (Unit Tests)
- Test business logic
- Mock repositories
- Test edge cases (concurrency, validation)

### `integration/` (Integration Tests)
- Test full application flow
- Use TestContainers for PostgreSQL and Redis
- Test actual database transactions and caching

---

## Documentation (`docs/`)

### `PRD.md`
Product Requirements Document with:
- Business scenarios
- Functional requirements
- Non-functional requirements
- Edge cases and success criteria

### `ARCHITECTURE.md`
System architecture documentation with:
- Component diagrams
- Database schema
- Sequence diagrams
- Technology stack

### `IMPLEMENTATION_PLAN.md`
Development roadmap (internal use).

---

## Summary

This structure follows **clean architecture** principles:

1. **Separation of Concerns:** Clear layering (controller → service → repository)
2. **Dependency Injection:** Spring manages all dependencies
3. **SOLID Principles:** Single responsibility, open/closed, interface segregation
4. **Testability:** Each layer can be tested independently
5. **Scalability:** Stateless design, connection pooling, caching

**Key Design Patterns:**
- **Repository Pattern:** Data access abstraction
- **Service Layer:** Business logic encapsulation
- **DTO Pattern:** Decoupling API contracts from entities
- **Builder Pattern:** Immutable DTOs (Lombok)
- **Circuit Breaker:** Fault tolerance (Resilience4j)
- **Cache-Aside:** Redis caching
- **Optimistic Locking:** Concurrency control
- **Scheduled Jobs:** Background processing with distributed locking

---

**Last Updated:** March 15, 2026  
**Maintained By:** SkyHigh Airlines Engineering Team

