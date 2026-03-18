# SkyHigh Core - Architecture Document

**Version:** 1.0
**Date:** March 15, 2026

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [High-Level Architecture](#2-high-level-architecture)
3. [Component Architecture](#3-component-architecture)
4. [Database Schema](#4-database-schema)
5. [Technology Stack](#5-technology-stack)
6. [Data Flow](#6-data-flow)
7. [Concurrency Control](#7-concurrency-control)
8. [Caching Strategy](#8-caching-strategy)
9. [Background Processing](#9-background-processing)
10. [Scalability and Performance](#10-scalability-and-performance)
11. [Security Considerations](#11-security-considerations)
12. [Deployment Architecture](#12-deployment-architecture)
13. [Summary](#13-summary)

---

## 1. System Overview

### Purpose

SkyHigh Core is a distributed digital check-in system designed to handle high-concurrency seat reservations, baggage validation, and payment processing for airline passengers.

### Key Requirements

| Requirement | Detail |
|---|---|
| Conflict-Free Seat Assignment | No double bookings under concurrent load |
| Time-Bound Reservations | 120-second seat holds with automatic expiration |
| High Performance | P95 under 1 second for seat map retrieval |
| Scalability | Support 500+ concurrent users |
| Reliability | 99.9% uptime during check-in windows |

---

## 2. High-Level Architecture

### System Context

```
[Passenger Mobile App]        [Airport Kiosk]
         |                          |
         |        HTTPS REST        |
         +----------+---------------+
                    |
            [Load Balancer]
                    |
         +----------+----------+
         |                     |
 [Spring Boot REST API]        |
  - CheckIn Controller         |
  - Seat Controller            |
  - Baggage Controller         |
  - Payment Controller         |
         |                     |
 [Business Logic Layer]        |
 [Data Access Layer]           |
         |                     |
 [Background Jobs]    [Redis Cache]
  (ShedLock)           (TTL 2s)
         |
   +-----+-----+
   |           |
[PostgreSQL] [Redis]
(Primary DB) (Cache)
```

---

## 3. Component Architecture

### Layered Architecture

```
+--------------------------------------------------+
|              PRESENTATION LAYER                  |
|  CheckInController  |  FlightController          |
|  SeatController     |  BaggageController         |
|  PaymentController                               |
+--------------------------------------------------+
                       |
               REST API - JSON over HTTP
                       |
+--------------------------------------------------+
|               SERVICE LAYER                      |
|  CheckInService  |  SeatService                  |
|  SeatHoldService |  BaggageService               |
|  PaymentService  |  FlightService                |
|                                                  |
|  Responsibilities:                               |
|  - Business rule validation                      |
|  - Transaction management (@Transactional)       |
|  - Orchestration between entities                |
|  - Exception handling                            |
+--------------------------------------------------+
                       |
+--------------------------------------------------+
|          DATA ACCESS LAYER (Repositories)        |
|  PassengerRepository  |  FlightRepository        |
|  SeatRepository       |  CheckInRepository       |
|  SeatHoldRepository   |  BaggageRepository       |
|                                                  |
|  Responsibilities:                               |
|  - CRUD via Spring Data JPA                      |
|  - Custom JPQL queries                           |
|  - Optimistic locking                            |
+--------------------------------------------------+
                       |
+--------------------------------------------------+
|              DOMAIN MODEL LAYER                  |
|  Entities: Passenger, Flight, Seat, CheckIn,     |
|            SeatHold, Baggage                     |
|  Enums:    SeatStatus, CheckInStatus,            |
|            SeatClass, HoldStatus, PaymentStatus  |
+--------------------------------------------------+
```

### Cross-Cutting Concerns

| Concern | Technology |
|---|---|
| Caching | Redis with @Cacheable |
| Logging | SLF4J + Logback |
| Exception Handling | @ControllerAdvice / GlobalExceptionHandler |
| Scheduling | Spring Scheduler + ShedLock |
| Input Validation | Jakarta Validation (@Valid) |
| Monitoring | Spring Boot Actuator |

---

## 4. Database Schema

### Entity Relationships

```
passengers
  - passenger_id (PK)
  - first_name
  - last_name
  - email
  - phone
        |
        | one passenger --> many check-ins
        |
check_ins
  - check_in_id (PK)
  - passenger_id (FK -> passengers)
  - flight_id    (FK -> flights)
  - booking_reference
  - seat_number
  - status
  - baggage_weight
  - payment_required
  - payment_status
  - created_at
  - completed_at
        |
        | one check-in --> many baggage records
        |
baggage
  - baggage_id (PK)
  - check_in_id (FK -> check_ins)
  - weight
  - pieces
  - excess_weight
  - excess_fee
  - payment_transaction_id
  - validated_at


flights
  - flight_id (PK)
  - flight_number
  - origin
  - destination
  - departure_time
  - arrival_time
  - aircraft_type
  - status
  - check_in_opens_at
  - check_in_closes_at
        |
        | one flight --> many seats
        |
seats
  - seat_id (PK)
  - flight_id (FK -> flights)
  - seat_number
  - row_number
  - column_letter
  - seat_class
  - position
  - status
  - price
  - version  <-- optimistic locking field
        |
        | one seat --> many holds
        |
seat_holds
  - hold_id (PK)
  - seat_id     (FK -> seats)
  - check_in_id (FK -> check_ins)
  - status
  - held_at
  - expires_at  (= held_at + 120 seconds)
  - confirmed_at
```

---

### Table Definitions

#### passengers

| Column | Type | Constraints | Description |
|---|---|---|---|
| passenger_id | VARCHAR(50) | PK | Unique passenger ID |
| first_name | VARCHAR(100) | NOT NULL | First name |
| last_name | VARCHAR(100) | NOT NULL | Last name |
| email | VARCHAR(150) | NOT NULL | Email address |
| phone | VARCHAR(20) | optional | Phone number |

---

#### flights

| Column | Type | Constraints | Description |
|---|---|---|---|
| flight_id | VARCHAR(20) | PK | Unique flight ID |
| flight_number | VARCHAR(10) | NOT NULL | e.g. SK123 |
| origin | VARCHAR(3) | NOT NULL | Origin airport code |
| destination | VARCHAR(3) | NOT NULL | Destination airport code |
| departure_time | TIMESTAMP | NOT NULL | Departure datetime |
| arrival_time | TIMESTAMP | NOT NULL | Arrival datetime |
| aircraft_type | VARCHAR(20) | NOT NULL | Aircraft model |
| status | VARCHAR(20) | NOT NULL | Flight status enum |
| check_in_opens_at | TIMESTAMP | optional | Check-in window open time |
| check_in_closes_at | TIMESTAMP | optional | Check-in window close time |

---

#### seats

| Column | Type | Constraints | Description |
|---|---|---|---|
| seat_id | UUID | PK | Unique seat ID |
| flight_id | VARCHAR(20) | FK flights | Associated flight |
| seat_number | VARCHAR(10) | NOT NULL | e.g. 12A |
| row_number | INTEGER | NOT NULL | Row number |
| column_letter | VARCHAR(2) | NOT NULL | Column letter |
| seat_class | VARCHAR(20) | NOT NULL | ECONOMY / BUSINESS / FIRST |
| position | VARCHAR(20) | NOT NULL | WINDOW / MIDDLE / AISLE |
| status | VARCHAR(20) | NOT NULL | AVAILABLE / HELD / CONFIRMED |
| price | DECIMAL(10,2) | NOT NULL | Seat price |
| version | BIGINT | DEFAULT 0 | Optimistic locking version |

**Indexes:**
- Primary key on seat_id
- Composite index on (flight_id, seat_number) for fast lookups
- Index on status for filtering available seats

---

#### check_ins

| Column | Type | Constraints | Description |
|---|---|---|---|
| check_in_id | UUID | PK | Unique check-in ID |
| passenger_id | VARCHAR(50) | FK passengers | Passenger reference |
| flight_id | VARCHAR(20) | FK flights | Flight reference |
| booking_reference | VARCHAR(10) | NOT NULL | Booking confirmation code |
| seat_number | VARCHAR(10) | optional | Assigned seat after selection |
| status | VARCHAR(20) | NOT NULL | INITIATED / AWAITING_PAYMENT / COMPLETED |
| baggage_weight | DECIMAL(10,2) | optional | Declared baggage weight |
| payment_required | BOOLEAN | optional | Excess baggage fee required |
| payment_status | VARCHAR(20) | optional | Payment status |
| created_at | TIMESTAMP | NOT NULL | Check-in start time |
| completed_at | TIMESTAMP | optional | Check-in completion time |

**Unique Constraint:** (passenger_id, flight_id) - one check-in per passenger per flight

---

#### seat_holds

| Column | Type | Constraints | Description |
|---|---|---|---|
| hold_id | UUID | PK | Unique hold ID |
| seat_id | UUID | FK seats | Seat being held |
| check_in_id | UUID | FK check_ins | Associated check-in |
| status | VARCHAR(20) | NOT NULL | HELD / CONFIRMED / EXPIRED |
| held_at | TIMESTAMP | NOT NULL | Hold start time |
| expires_at | TIMESTAMP | NOT NULL | held_at + 120 seconds |
| confirmed_at | TIMESTAMP | optional | Confirmation timestamp |

**Indexes:**
- Composite index on (status, expires_at) used by the expiration job

---

#### baggage

| Column | Type | Constraints | Description |
|---|---|---|---|
| baggage_id | UUID | PK | Unique baggage ID |
| check_in_id | UUID | FK check_ins | Associated check-in |
| weight | DECIMAL(10,2) | NOT NULL | Total weight in kg |
| pieces | INTEGER | NOT NULL | Number of pieces |
| excess_weight | DECIMAL(10,2) | optional | Weight over 25 kg |
| excess_fee | DECIMAL(10,2) | optional | Fee for excess weight |
| payment_transaction_id | VARCHAR(100) | optional | Payment transaction reference |
| validated_at | TIMESTAMP | NOT NULL | Validation timestamp |

---

#### shedlock

| Column | Type | Constraints | Description |
|---|---|---|---|
| name | VARCHAR(64) | PK | Lock name / job identifier |
| lock_until | TIMESTAMP | NOT NULL | Lock valid until this time |
| locked_at | TIMESTAMP | NOT NULL | Lock acquisition time |
| locked_by | VARCHAR(255) | NOT NULL | Instance that holds the lock |

---

## 5. Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Backend Framework | Spring Boot | 3.2.3 | Application framework |
| Language | Java | 17 | Programming language |
| Build Tool | Maven | 3.8+ | Dependency management and build |
| Database | PostgreSQL | 15 | Primary relational database |
| Cache | Redis | 7 | In-memory cache |
| ORM | Hibernate / JPA | 6.x | Object-relational mapping |
| Migration | Flyway | Latest | Database schema versioning |
| Scheduler | Spring Scheduler | 3.2.3 | Background job scheduling |
| Distributed Lock | ShedLock | 5.9.0 | Prevent duplicate job execution |
| Resilience | Resilience4j | 2.1.0 | Circuit breaker and retry |
| API Docs | Swagger / OpenAPI | 3.x | API documentation |
| Logging | SLF4J + Logback | Latest | Structured logging |
| Testing | JUnit 5 + Mockito | Latest | Unit and integration testing |
| Containerization | Docker + Docker Compose | Latest | Packaging and deployment |

### Key Dependencies

```
Spring Boot Starters:
  spring-boot-starter-web
  spring-boot-starter-data-jpa
  spring-boot-starter-data-redis
  spring-boot-starter-validation
  spring-boot-starter-cache
  spring-boot-starter-actuator

Database:
  postgresql
  flyway-core

Utilities:
  lombok

Resilience:
  resilience4j-spring-boot3
  resilience4j-circuitbreaker
  resilience4j-retry

Distributed Locking:
  shedlock-spring
  shedlock-provider-jdbc-template
```

---

## 6. Data Flow

### Seat Hold Flow (Step by Step)

```
Client
  |
  |  POST /api/v1/seats/hold
  |  Body: { seatId, checkInId }
  |
  v
SeatController
  - Validates request with @Valid
  |
  v
SeatHoldService  (@Transactional)
  |
  Step 1: Find seat WHERE seat_id = ? AND status = AVAILABLE
          --> If not found, throw SeatUnavailableException
  |
  Step 2: Update seat status to HELD
          UPDATE seats
          SET status = HELD, version = version + 1
          WHERE seat_id = ? AND version = ?
          --> If 0 rows updated, throw OptimisticLockException
              (another request got there first)
  |
  Step 3: Create SeatHold record
          hold_id    = new UUID
          status     = HELD
          held_at    = now()
          expires_at = now() + 120 seconds
  |
  Step 4: INSERT into seat_holds table
  |
  Step 5: Commit transaction
  |
  v
Return HoldSeatResponse
  { holdId, seatNumber, expiresAt }
```

---

## 7. Concurrency Control

### Problem

Multiple passengers can try to hold the same seat at exactly the same time. Without protection, two passengers could both get confirmed for seat 12A.

### Solution: Optimistic Locking

Every row in the seats table has a `version` column. Every time the row is updated, the version increments by 1. Before updating, the query checks the version matches what was read. If another request already updated it, the version will not match and the update fails.

### Timeline Example

```
Time    Passenger A              Database (seat 12A)     Passenger B
----    -----------              -------------------     -----------

T0      Read seat                version = 5             Read seat
        (version = 5)                                    (version = 5)

T1      Set status = HELD

T2      UPDATE seats             Version becomes 6       
        WHERE version = 5        SUCCESS for A
        --> 1 row updated

T3                                                       UPDATE seats
                                                         WHERE version = 5
                                                         --> 0 rows updated
                                                         FAIL for B
T4                               Version is now 6        SeatUnavailableException
```

### JPA Configuration

```java
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    private UUID seatId;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @Version
    private Long version;  // Spring/JPA handles incrementing automatically
}
```

### Transaction Isolation Level

```yaml
spring:
  jpa:
    properties:
      hibernate:
        connection:
          isolation: 2   # READ_COMMITTED
```

---

## 8. Caching Strategy

### Why Cache?

The seat map endpoint is the most frequently called API. Querying the full seat list for a flight on every request would overload the database under high concurrency.

### Cache-Aside Pattern

```
Client
  |
  |  GET /api/v1/flights/{flightId}/seats
  |
  v
SeatService
  |
  |-- Check Redis for key: seatMap:{flightId}
  |
  +-- CACHE HIT
  |     Return data immediately (no DB query)
  |
  +-- CACHE MISS
        Query PostgreSQL:
          SELECT * FROM seats WHERE flight_id = ?
        Save result to Redis with TTL = 2 seconds
        Return data to client
```

### Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
  cache:
    type: redis
    redis:
      time-to-live: 2000
```

### Service Code

```java
@Service
public class SeatService {

    @Cacheable(value = "seatMap", key = "#flightId")
    public List<Seat> getSeatMap(String flightId) {
        return seatRepository.findByFlightId(flightId);
    }

    @CacheEvict(value = "seatMap", key = "#flightId")
    public void invalidateSeatMap(String flightId) {
        // called after any seat status change
    }
}
```

**Note:** The 2-second TTL is a deliberate trade-off. It keeps read latency under the P95 target of 1 second while still showing near-real-time seat availability.

---

## 9. Background Processing

### Seat Hold Expiration Job

**Purpose:** Release seats that have been held for more than 120 seconds so other passengers can book them.

**Frequency:** Runs every 10 seconds.

### Flow

```
Every 10 seconds:
  Spring Scheduler triggers SeatHoldExpirationJob
    |
    v
  ShedLock checks: is this job already running on another instance?
    |
    +-- YES --> Skip. Do nothing.
    |
    +-- NO  --> Acquire lock and run job:
                  |
                  Step 1: Find all expired holds
                          SELECT * FROM seat_holds
                          WHERE status = 'HELD'
                          AND expires_at < NOW()
                  |
                  Step 2: For each expired hold:
                          - Set seat status:  HELD --> AVAILABLE
                          - Set hold status:  HELD --> EXPIRED
                          - Save both records
                  |
                  Step 3: Log how many holds were expired
                  |
                  Release lock
```

### ShedLock Settings

| Setting | Value | Purpose |
|---|---|---|
| lockAtLeastFor | 5 seconds | Minimum time the lock is held |
| lockAtMostFor | 9 seconds | Maximum time before lock auto-releases (failsafe) |
| Schedule interval | 10 seconds | How often the job runs |

### Code

```java
@Component
@Slf4j
public class SeatHoldExpirationJob {

    @Scheduled(fixedRateString = "${app.seat-hold.expiration-job-rate}")
    @SchedulerLock(
        name = "SeatHoldExpirationJob",
        lockAtLeastFor = "5s",
        lockAtMostFor = "9s"
    )
    public void expireHolds() {
        List<SeatHold> expired = seatHoldRepository.findExpiredHolds(Instant.now());

        for (SeatHold hold : expired) {
            Seat seat = seatRepository.findById(hold.getSeatId()).orElseThrow();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);

            hold.setStatus(HoldStatus.EXPIRED);
            seatHoldRepository.save(hold);

            log.info("Expired hold {} for seat {}", hold.getHoldId(), seat.getSeatNumber());
        }

        log.info("Total holds expired this run: {}", expired.size());
    }
}
```

---

## 10. Scalability and Performance

### Horizontal Scaling

The application is fully stateless. No session or user data is stored in application memory. Multiple instances can run simultaneously behind a load balancer, all sharing the same PostgreSQL database and Redis cache.

```
          [Load Balancer]
                |
    +-----------+-----------+
    |           |           |
[Instance 1] [Instance 2] [Instance 3]
    |           |           |
    +-----+-----+-----------+
          |           |
    [PostgreSQL]   [Redis]
     Shared DB    Shared Cache
```

### Database Connection Pool (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Redis Connection Pool (Lettuce)

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### Database Indexes

| Index Name | Columns | Why |
|---|---|---|
| idx_seat_flight_number | (flight_id, seat_number) | Fast individual seat lookup |
| idx_seat_status | status | Filter available seats quickly |
| idx_hold_status_expires | (status, expires_at) | Expiration job performance |

---

## 11. Security Considerations

### Currently Implemented

| Area | How |
|---|---|
| Input Validation | Jakarta Bean Validation on all request DTOs |
| SQL Injection Prevention | JPA parameterized queries - no raw SQL |
| Safe Error Responses | Exception handler strips sensitive info from responses |

### Planned for Future

1. **Authentication** - JWT-based token authentication
2. **Authorization** - Role-based access control (passenger vs staff vs admin)
3. **Rate Limiting** - Per-IP and per-user request throttling
4. **HTTPS** - TLS termination at load balancer
5. **Encryption at Rest** - Sensitive passenger data encrypted in DB

---

## 12. Deployment Architecture

### Local Development (Docker Compose)

```yaml
services:
  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skyhigh_db
      SPRING_DATA_REDIS_HOST: redis
```

### Production (Cloud / Kubernetes)

```
[Application Load Balancer]
            |
  +---------+---------+
  |         |         |
[Pod 1]  [Pod 2]  [Pod 3]   <-- Kubernetes Deployment / Auto Scaling
  |         |         |
  +---------+---------+
            |
  +---------+---------+
  |                   |
[RDS PostgreSQL]  [ElastiCache Redis]
  Multi-AZ            Multi-AZ
  Read Replica        Cluster Mode
```

---

## 13. Summary

### Architecture at a Glance

| Layer | Technology | Role |
|---|---|---|
| API | Spring Boot REST | Handles HTTP requests |
| Business Logic | Spring Services | Enforces business rules |
| Data Access | Spring Data JPA | Database operations |
| Database | PostgreSQL | Persistent storage with ACID |
| Cache | Redis | Fast seat map reads |
| Scheduler | Spring + ShedLock | Background hold expiration |
| Resilience | Resilience4j | Circuit breaker and retry |

### Key Design Decisions

| Decision | Reason |
|---|---|
| Optimistic Locking | Handles concurrent seat holds without database-level locks, better performance at scale |
| 2-second Cache TTL | Achieves P95 under 1s read latency while keeping seat map data fresh enough |
| 120-second Hold Timer | Gives passengers enough time to complete check-in without permanently blocking a seat |
| ShedLock for Scheduling | Ensures only one instance runs the expiration job even when multiple app instances are running |
| Stateless Application | Allows horizontal scaling - any instance can handle any request |
| PostgreSQL | ACID transactions needed for payment and seat assignment correctness |
| Redis | Sub-millisecond reads needed for high-traffic seat map endpoint |

---

**Last Updated:** March 15, 2026
**Maintained By:** SkyHigh Airlines Engineering Team

