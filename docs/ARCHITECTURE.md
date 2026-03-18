# Architecture Document
# SkyHigh Core - Digital Check-In System

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
10. [Scalability & Performance](#10-scalability--performance)
11. [Security Considerations](#11-security-considerations)
12. [Deployment Architecture](#12-deployment-architecture)

---

## 1. System Overview

### Purpose

SkyHigh Core is a distributed digital check-in system designed to handle high-concurrency seat reservations, baggage validation, and payment processing for airline passengers.

### Key Requirements

- **Conflict-Free Seat Assignment:** No double bookings under concurrent load
- **Time-Bound Reservations:** 120-second seat holds with automatic expiration
- **High Performance:** P95 < 1 second for seat map retrieval
- **Scalability:** Support 500+ concurrent users
- **Reliability:** 99.9% uptime during check-in windows

---

## 2. High-Level Architecture

### System Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     External Systems                            │
│                                                                 │
│  ┌──────────────┐         ┌──────────────┐                     │
│  │   Passenger  │         │   Airport    │                     │
│  │   (Mobile)   │         │    Kiosk     │                     │
│  └───────┬──────┘         └───────┬──────┘                     │
│          │                        │                            │
└──────────┼────────────────────────┼────────────────────────────┘
           │                        │
           │      HTTPS / REST      │
           │                        │
┌──────────▼────────────────────────▼────────────────────────────┐
│                  Load Balancer (Future)                        │
└───────────────────────────┬────────────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────────────┐
│               SkyHigh Core Application Layer                   │
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐   │
│  │          Spring Boot REST API (Stateless)              │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │ Check-In │  │   Seat   │  │ Baggage  │             │   │
│  │  │Controller│  │Controller│  │Controller│             │   │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘             │   │
│  │       │             │             │                    │   │
│  │  ┌────▼─────────────▼─────────────▼─────┐             │   │
│  │  │        Business Logic Layer          │             │   │
│  │  │  (Services with Transaction Mgmt)    │             │   │
│  │  └────┬─────────────┬─────────────┬─────┘             │   │
│  │       │             │             │                    │   │
│  │  ┌────▼─────────────▼─────────────▼─────┐             │   │
│  │  │     Data Access Layer (Repositories) │             │   │
│  │  └────┬─────────────┬─────────────┬─────┘             │   │
│  └───────┼─────────────┼─────────────┼────────────────────┘   │
│          │             │             │                        │
│  ┌───────▼─────┐       │       ┌─────▼──────────┐            │
│  │ Background  │       │       │   Cache Layer  │            │
│  │    Jobs     │       │       │   (Redis)      │            │
│  │ (Scheduler) │       │       └────────────────┘            │
│  └─────────────┘       │                                      │
└────────────────────────┼──────────────────────────────────────┘
                         │
                         │
┌────────────────────────▼──────────────────────────────────────┐
│                  Persistence Layer                            │
│                                                                │
│  ┌──────────────────┐          ┌──────────────────┐          │
│  │   PostgreSQL     │          │      Redis       │          │
│  │   (Primary DB)   │          │     (Cache)      │          │
│  │                  │          │                  │          │
│  │  - ACID trans    │          │  - TTL: 2s       │          │
│  │  - Optimistic    │          │  - Seat maps     │          │
│  │    locking       │          │                  │          │
│  └──────────────────┘          └──────────────────┘          │
└───────────────────────────────────────────────────────────────┘
```

---

## 3. Component Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌──────────┐│
│  │ CheckIn   │  │  Flight   │  │   Seat    │  │ Baggage  ││
│  │Controller │  │ Controller│  │ Controller│  │Controller││
│  └───────────┘  └───────────┘  └───────────┘  └──────────┘│
│        │              │              │              │       │
│        └──────────────┴──────────────┴──────────────┘       │
│                         │                                   │
│              REST API (JSON over HTTP)                      │
└─────────────────────────┼───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Service Layer (Business Logic)           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  CheckIn     │  │    Seat      │  │   Baggage    │     │
│  │  Service     │  │  Service     │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  SeatHold    │  │   Payment    │  │   Flight     │     │
│  │  Service     │  │  Service     │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  Responsibilities:                                          │
│  - Business rule validation                                 │
│  - Transaction management (@Transactional)                  │
│  - Orchestration between entities                           │
│  - Exception handling                                       │
└─────────────────────────┼───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│              Data Access Layer (Repositories)               │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐ │
│  │ Passenger  │ │   Flight   │ │    Seat    │ │CheckIn  │ │
│  │ Repository │ │ Repository │ │ Repository │ │Repositry│ │
│  └────────────┘ └────────────┘ └────────────┘ └─────────┘ │
│  ┌────────────┐ ┌────────────┐                             │
│  │  SeatHold  │ │  Baggage   │                             │
│  │ Repository │ │ Repository │                             │
│  └────────────┘ └────────────┘                             │
│                                                             │
│  Responsibilities:                                          │
│  - CRUD operations (Spring Data JPA)                        │
│  - Custom queries                                           │
│  - Optimistic locking                                       │
└─────────────────────────┼───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Domain Model Layer                       │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────┐ │
│  │ Passenger  │ │   Flight   │ │    Seat    │ │CheckIn  │ │
│  │  Entity    │ │   Entity   │ │   Entity   │ │ Entity  │ │
│  └────────────┘ └────────────┘ └────────────┘ └─────────┘ │
│  ┌────────────┐ ┌────────────┐                             │
│  │  SeatHold  │ │  Baggage   │                             │
│  │   Entity   │ │   Entity   │                             │
│  └────────────┘ └────────────┘                             │
│                                                             │
│  + Enums: SeatStatus, CheckInStatus, SeatClass, etc.       │
└─────────────────────────────────────────────────────────────┘
```

### Cross-Cutting Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                  Cross-Cutting Concerns                     │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Caching   │  │   Logging   │  │  Exception  │        │
│  │  (Redis)    │  │  (Slf4j)    │  │   Handling  │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Scheduling  │  │ Validation  │  │  Monitoring │        │
│  │ (ShedLock)  │  │  (Jakarta)  │  │  (Actuator) │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Database Schema

### Entity Relationship Diagram (ERD)

```
┌──────────────────┐
│   passengers     │
├──────────────────┤
│ passenger_id (PK)│───┐
│ first_name       │   │
│ last_name        │   │
│ email            │   │
│ phone            │   │
└──────────────────┘   │
                       │
                       │ 1
                       │
                       │ *
┌──────────────────┐   │      ┌──────────────────┐
│     flights      │   │      │   check_ins      │
├──────────────────┤   │      ├──────────────────┤
│ flight_id (PK)   │───┼─────▶│ check_in_id (PK) │
│ flight_number    │   │   ┌──│ passenger_id (FK)│
│ origin           │   │   │  │ flight_id (FK)   │
│ destination      │   │   │  │ booking_reference│
│ departure_time   │   └───┘  │ seat_number      │
│ arrival_time     │          │ status           │
│ status           │          │ baggage_weight   │
│ check_in_opens_at│          │ payment_required │
│check_in_closes_at│          │ payment_status   │
└──────────────────┘          │ created_at       │
        │                     │ completed_at     │
        │ 1                   └────────┬─────────┘
        │                              │
        │ *                            │ 1
        │                              │
┌───────▼──────────┐                   │ *
│      seats       │                   │
├──────────────────┤          ┌────────▼─────────┐
│ seat_id (PK)     │───┐      │    baggage       │
│ flight_id (FK)   │   │      ├──────────────────┤
│ seat_number      │   │      │ baggage_id (PK)  │
│ row_number       │   │      │ check_in_id (FK) │
│ column_letter    │   │      │ weight           │
│ seat_class       │   │      │ pieces           │
│ position         │   │      │ excess_weight    │
│ status           │   │      │ excess_fee       │
│ price            │   │      │ payment_txn_id   │
│ version ⚡       │   │      │ validated_at     │
└──────────────────┘   │      └──────────────────┘
                       │
                       │ 1
                       │
                       │ *
              ┌────────▼─────────┐
              │   seat_holds     │
              ├──────────────────┤
              │ hold_id (PK)     │
              │ seat_id (FK)     │
              │ check_in_id (FK) │
              │ status           │
              │ held_at          │
              │ expires_at       │
              │ confirmed_at     │
              └──────────────────┘

⚡ = Optimistic locking version field
```

### Table Definitions

#### `passengers`

| Column        | Type         | Constraints | Description              |
|---------------|--------------|-------------|--------------------------|
| passenger_id  | VARCHAR(50)  | PK          | Unique passenger ID      |
| first_name    | VARCHAR(100) | NOT NULL    | First name               |
| last_name     | VARCHAR(100) | NOT NULL    | Last name                |
| email         | VARCHAR(150) | NOT NULL    | Email address            |
| phone         | VARCHAR(20)  |             | Phone number (optional)  |

---

#### `flights`

| Column             | Type         | Constraints | Description                 |
|--------------------|--------------|-------------|-----------------------------|
| flight_id          | VARCHAR(20)  | PK          | Unique flight ID            |
| flight_number      | VARCHAR(10)  | NOT NULL    | Flight number (e.g., SK123) |
| origin             | VARCHAR(3)   | NOT NULL    | Origin airport code         |
| destination        | VARCHAR(3)   | NOT NULL    | Destination airport code    |
| departure_time     | TIMESTAMP    | NOT NULL    | Departure datetime          |
| arrival_time       | TIMESTAMP    | NOT NULL    | Arrival datetime            |
| aircraft_type      | VARCHAR(20)  | NOT NULL    | Aircraft model              |
| status             | VARCHAR(20)  | NOT NULL    | Flight status (enum)        |
| check_in_opens_at  | TIMESTAMP    |             | Check-in window opens       |
| check_in_closes_at | TIMESTAMP    |             | Check-in window closes      |

**Indexes:**
- Primary key: `flight_id`

---

#### `seats`

| Column        | Type          | Constraints       | Description                     |
|---------------|---------------|-------------------|---------------------------------|
| seat_id       | UUID          | PK                | Unique seat ID                  |
| flight_id     | VARCHAR(20)   | FK (flights)      | Associated flight               |
| seat_number   | VARCHAR(10)   | NOT NULL          | Seat number (e.g., "12A")       |
| row_number    | INTEGER       | NOT NULL          | Row number                      |
| column_letter | VARCHAR(2)    | NOT NULL          | Column letter                   |
| seat_class    | VARCHAR(20)   | NOT NULL          | ECONOMY, BUSINESS, FIRST        |
| position      | VARCHAR(20)   | NOT NULL          | WINDOW, MIDDLE, AISLE           |
| status        | VARCHAR(20)   | NOT NULL          | AVAILABLE, HELD, CONFIRMED      |
| price         | DECIMAL(10,2) | NOT NULL          | Seat price                      |
| **version**   | **BIGINT**    | **DEFAULT 0**     | **Optimistic locking version**  |

**Indexes:**
- Primary key: `seat_id`
- Composite: `(flight_id, seat_number)` for fast lookups
- Index: `status` for filtering available seats

**Concurrency Control:**
- `version` field enables optimistic locking
- Increments on every update
- Prevents concurrent modifications

---

#### `check_ins`

| Column            | Type          | Constraints           | Description                    |
|-------------------|---------------|-----------------------|--------------------------------|
| check_in_id       | UUID          | PK                    | Unique check-in ID             |
| passenger_id      | VARCHAR(50)   | FK (passengers)       | Passenger reference            |
| flight_id         | VARCHAR(20)   | FK (flights)          | Flight reference               |
| booking_reference | VARCHAR(10)   | NOT NULL              | Booking confirmation code      |
| seat_number       | VARCHAR(10)   |                       | Assigned seat (after selection)|
| status            | VARCHAR(20)   | NOT NULL              | INITIATED, AWAITING_PAYMENT, etc.|
| baggage_weight    | DECIMAL(10,2) |                       | Declared baggage weight        |
| payment_required  | BOOLEAN       |                       | Excess baggage fee required?   |
| payment_status    | VARCHAR(20)   |                       | Payment status                 |
| created_at        | TIMESTAMP     | NOT NULL              | Check-in start time            |
| completed_at      | TIMESTAMP     |                       | Check-in completion time       |

**Unique Constraint:**
- `(passenger_id, flight_id)` - One check-in per passenger per flight

**Indexes:**
- Primary key: `check_in_id`
- Unique: `(passenger_id, flight_id)`
- Index: `status` for filtering

---

#### `seat_holds`

| Column        | Type       | Constraints       | Description                     |
|---------------|------------|-------------------|---------------------------------|
| hold_id       | UUID       | PK                | Unique hold ID                  |
| seat_id       | UUID       | FK (seats)        | Seat being held                 |
| check_in_id   | UUID       | FK (check_ins)    | Associated check-in             |
| status        | VARCHAR(20)| NOT NULL          | HELD, CONFIRMED, EXPIRED        |
| held_at       | TIMESTAMP  | NOT NULL          | Hold start time                 |
| expires_at    | TIMESTAMP  | NOT NULL          | Hold expiration time (held_at + 120s)|
| confirmed_at  | TIMESTAMP  |                   | Confirmation timestamp          |

**Business Rule:**
- `expires_at = held_at + 120 seconds`

**Indexes:**
- Primary key: `hold_id`
- Index: `seat_id` for seat lookup
- Index: `check_in_id` for check-in lookup
- Composite: `(status, expires_at)` for expiration job

---

#### `baggage`

| Column              | Type          | Constraints       | Description                     |
|---------------------|---------------|-------------------|---------------------------------|
| baggage_id          | UUID          | PK                | Unique baggage ID               |
| check_in_id         | UUID          | FK (check_ins)    | Associated check-in             |
| weight              | DECIMAL(10,2) | NOT NULL          | Total weight (kg)               |
| pieces              | INTEGER       | NOT NULL          | Number of pieces                |
| excess_weight       | DECIMAL(10,2) |                   | Weight over 25kg                |
| excess_fee          | DECIMAL(10,2) |                   | Fee for excess weight           |
| payment_transaction_id | VARCHAR(100)|                | Payment transaction reference   |
| validated_at        | TIMESTAMP     | NOT NULL          | Validation timestamp            |

**Indexes:**
- Primary key: `baggage_id`
- Index: `check_in_id` for lookup

---

#### `shedlock` (Distributed Locking)

| Column      | Type         | Constraints | Description                 |
|-------------|--------------|-------------|-----------------------------|
| name        | VARCHAR(64)  | PK          | Lock name (job identifier)  |
| lock_until  | TIMESTAMP    | NOT NULL    | Lock valid until            |
| locked_at   | TIMESTAMP    | NOT NULL    | Lock acquisition time       |
| locked_by   | VARCHAR(255) | NOT NULL    | Instance identifier         |

**Purpose:** Ensures only one instance of a scheduled job runs at a time.

---

## 5. Technology Stack

### Core Technologies

| Layer                | Technology             | Version | Purpose                          |
|----------------------|------------------------|---------|----------------------------------|
| **Backend Framework**| Spring Boot            | 3.2.3   | Application framework            |
| **Language**         | Java                   | 17      | Programming language             |
| **Build Tool**       | Maven                  | 3.8+    | Dependency management & build    |
| **Database**         | PostgreSQL             | 15      | Primary relational database      |
| **Cache**            | Redis                  | 7       | In-memory cache                  |
| **ORM**              | Hibernate/JPA          | 6.x     | Object-relational mapping        |
| **Migration**        | Flyway                 | Latest  | Database schema versioning       |
| **Scheduler**        | Spring Scheduler       | 3.2.3   | Background job scheduling        |
| **Distributed Lock** | ShedLock               | 5.9.0   | Prevent duplicate job execution  |
| **Resilience**       | Resilience4j           | 2.1.0   | Circuit breaker, retry           |
| **API Docs**         | Swagger/OpenAPI        | 3.x     | API documentation                |
| **Logging**          | SLF4J + Logback        | Latest  | Structured logging               |
| **Testing**          | JUnit 5 + Mockito      | Latest  | Unit & integration testing       |
| **Containerization** | Docker + Docker Compose| Latest  | Application packaging & deployment|

---

### Key Libraries

```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-data-redis
spring-boot-starter-validation
spring-boot-starter-cache
spring-boot-starter-actuator

<!-- Database -->
postgresql (runtime)
flyway-core

<!-- Utilities -->
lombok (compile-time code generation)

<!-- Resilience -->
resilience4j-spring-boot3
resilience4j-circuitbreaker
resilience4j-retry

<!-- Distributed Locking -->
shedlock-spring
shedlock-provider-jdbc-template
```

---

## 6. Data Flow

### Seat Hold Data Flow

```
┌──────────┐
│ Client   │
└─────┬────┘
      │ 1. POST /api/v1/seats/hold
      │    {seatId, checkInId}
      ▼
┌─────────────────┐
│ SeatController  │
└─────┬───────────┘
      │ 2. Validate request
      │
      ▼
┌──────────────────┐
│ SeatHoldService  │
└─────┬────────────┘
      │ 3. @Transactional begin
      │
      ├──▶ SeatRepository.findBySeatIdAndStatus(seatId, AVAILABLE)
      │    ├─ SELECT * FROM seats
      │    │  WHERE seat_id = ? AND status = 'AVAILABLE' AND version = ?
      │    │  FOR UPDATE (optimistic lock)
      │    │
      │    └─ If not found: throw SeatUnavailableException
      │
      ├──▶ seat.setStatus(HELD)
      │    seat.setVersion(version + 1)
      │
      ├──▶ SeatRepository.save(seat)
      │    ├─ UPDATE seats
      │    │  SET status = 'HELD', version = version + 1
      │    │  WHERE seat_id = ? AND version = ?
      │    │
      │    └─ If rows affected = 0: throw OptimisticLockException
      │
      ├──▶ Create SeatHold entity
      │    ├─ holdId = UUID.randomUUID()
      │    ├─ seatId = seat.getSeatId()
      │    ├─ checkInId = request.getCheckInId()
      │    ├─ status = HELD
      │    ├─ heldAt = Instant.now()
      │    └─ expiresAt = heldAt.plusSeconds(120)
      │
      ├──▶ SeatHoldRepository.save(hold)
      │    └─ INSERT INTO seat_holds (...)
      │
      └──▶ @Transactional commit
           │
           ▼
      Return HoldResponse
```

---

## 7. Concurrency Control

### Optimistic Locking Strategy

**Problem:** Multiple passengers attempting to hold the same seat simultaneously.

**Solution:** Optimistic locking with version field.

#### How It Works

```
Time    Passenger A                    Database                    Passenger B
─────   ────────────────────────────   ────────────────────────   ────────────────────
T0      Read seat 12A (version=5)      seat 12A: version=5        Read seat 12A (version=5)
        
T1      Modify: status = HELD          
        
T2      UPDATE seats                   UPDATE executed
        SET status='HELD',             version incremented to 6
        version=6                      Passenger A: SUCCESS ✓
        WHERE id=? AND version=5
        
T3                                                                 UPDATE seats
                                                                   SET status='HELD',
                                                                   version=6
                                                                   WHERE id=? AND version=5
                                                                   
T4                                      No rows updated!           Passenger B: FAIL ✗
                                        (version is now 6, not 5)  OptimisticLockException
```

#### JPA Configuration

```java
@Entity
@Table(name = "seats")
public class Seat {
    @Id
    private UUID seatId;
    
    @Enumerated(EnumType.STRING)
    private SeatStatus status;
    
    @Version  // ← Enables optimistic locking
    private Long version;
    
    // ...
}
```

#### Transaction Isolation

```yaml
spring:
  jpa:
    properties:
      hibernate:
        connection:
          isolation: 2  # READ_COMMITTED
```

---

## 8. Caching Strategy

### Cache-Aside Pattern

**Use Case:** Seat map retrieval (most frequently accessed data)

```
┌──────────┐
│  Client  │
└─────┬────┘
      │ GET /api/v1/flights/{flightId}/seats
      ▼
┌─────────────┐
│SeatService  │
└─────┬───────┘
      │
      ├─ Check Redis cache: key="seatMap:{flightId}"
      │
      ▼
  ┌───────┐
  │ Cache │
  │ Hit?  │
  └───┬───┘
      │
      ├─ YES ────────────────────────────┐
      │                                  │
      │                                  ▼
      │                          Return cached data
      │                          (Fast path)
      │
      └─ NO ─────────────────────────────┐
                                         │
                                         ▼
                                 Query PostgreSQL
                                 SELECT * FROM seats
                                 WHERE flight_id = ?
                                         │
                                         ▼
                                 Store in Redis
                                 TTL = 2 seconds
                                         │
                                         ▼
                                 Return fresh data
```

### Redis Configuration

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
      time-to-live: 2000  # 2 seconds
```

### Service Implementation

```java
@Service
public class SeatService {
    
    @Cacheable(value = "seatMap", key = "#flightId")
    public List<Seat> getSeatMap(String flightId) {
        log.info("Cache MISS - Fetching from database: {}", flightId);
        return seatRepository.findByFlightId(flightId);
    }
}
```

### Cache Invalidation (Optional)

```java
@CacheEvict(value = "seatMap", key = "#flightId")
public void invalidateSeatMap(String flightId) {
    // Called when seat status changes
}
```

**Trade-off:** 2-second cache TTL balances performance vs. data freshness.

---

## 9. Background Processing

### Seat Hold Expiration Job

**Purpose:** Automatically release seats held for >120 seconds.

#### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│               Application Instance 1                        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Spring Scheduler (Cron: every 10s)                 │   │
│  └───────────┬─────────────────────────────────────────┘   │
│              │                                             │
│              ▼                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ShedLock - Acquire Lock "SeatHoldExpirationJob"   │   │
│  │  - lockAtLeastFor: 5s                               │   │
│  │  - lockAtMostFor: 9s                                │   │
│  └───────────┬─────────────────────────────────────────┘   │
│              │                                             │
└──────────────┼─────────────────────────────────────────────┘
               │
               ▼
      ┌────────────────┐
      │  PostgreSQL    │
      │  shedlock      │
      │  table         │
      └────────┬───────┘
               │ Lock acquired (or skip if locked)
               │
               ▼
    Execute job only on ONE instance
    ┌─────────────────────────────────┐
    │ 1. Find expired holds:          │
    │    SELECT * FROM seat_holds     │
    │    WHERE status='HELD'          │
    │      AND expires_at < NOW()     │
    │                                 │
    │ 2. For each expired hold:       │
    │    - Update seat: HELD→AVAILABLE│
    │    - Update hold: HELD→EXPIRED  │
    │                                 │
    │ 3. Log expiration               │
    └─────────────────────────────────┘
               │
               ▼
    Release lock after completion
```

#### Code Implementation

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
        log.info("Starting seat hold expiration job");
        
        List<SeatHold> expiredHolds = seatHoldRepository
            .findExpiredHolds(Instant.now());
        
        for (SeatHold hold : expiredHolds) {
            // Update seat status
            Seat seat = seatRepository.findById(hold.getSeatId()).orElseThrow();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
            
            // Update hold status
            hold.setStatus(HoldStatus.EXPIRED);
            seatHoldRepository.save(hold);
            
            log.info("Expired hold {} for seat {}", 
                hold.getHoldId(), seat.getSeatNumber());
        }
        
        log.info("Expired {} holds", expiredHolds.size());
    }
}
```

---

## 10. Scalability & Performance

### Horizontal Scaling

**Design Principle:** Stateless application instances

```
                     ┌──────────────┐
                     │Load Balancer │
                     └──────┬───────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
          ▼                 ▼                 ▼
    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │ App      │      │ App      │      │ App      │
    │Instance 1│      │Instance 2│      │Instance 3│
    └────┬─────┘      └────┬─────┘      └────┬─────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
        ┌──────────┐              ┌──────────┐
        │PostgreSQL│              │  Redis   │
        │ (Shared) │              │ (Shared) │
        └──────────┘              └──────────┘
```

**Key Features:**
- No session state in application
- Shared database and cache
- ShedLock prevents duplicate background jobs

---

### Performance Optimizations

#### 1. Database Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

#### 2. Redis Connection Pooling

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

#### 3. Database Indexes

- `idx_seat_flight_number` on `(flight_id, seat_number)`
- `idx_seat_status` on `status`
- `idx_hold_status_expires` on `(status, expires_at)`

#### 4. Query Optimization

```java
// Fetch only necessary fields
@Query("SELECT new com.skyhigh.core.dto.SeatDTO(s.seatId, s.seatNumber, s.status) " +
       "FROM Seat s WHERE s.flightId = :flightId")
List<SeatDTO> findSeatMapProjection(@Param("flightId") String flightId);
```

---

## 11. Security Considerations

### Current Implementation

- **Input Validation:** Jakarta Validation (`@NotNull`, `@Positive`, etc.)
- **SQL Injection Prevention:** Parameterized queries via JPA
- **Error Handling:** No sensitive data in error responses

### Future Enhancements

1. **Authentication:** JWT tokens
2. **Authorization:** Role-based access control (RBAC)
3. **Rate Limiting:** Prevent abuse
4. **HTTPS:** TLS encryption in transit
5. **Data Encryption:** Sensitive data encryption at rest

---

## 12. Deployment Architecture

### Docker Compose Deployment

```yaml
services:
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    volumes: [postgres_data:/var/lib/postgresql/data]
  
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    volumes: [redis_data:/data]
  
  app:
    build: .
    ports: ["8080:8080"]
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skyhigh_db
      SPRING_DATA_REDIS_HOST: redis
```

### Production Deployment (Future)

```
┌────────────────────────────────────────────────────────────┐
│                       Cloud Provider                       │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         Application Load Balancer (ALB)              │ │
│  └────────────────────┬─────────────────────────────────┘ │
│                       │                                    │
│  ┌────────────────────┼─────────────────────────────────┐ │
│  │  Auto Scaling Group │                                 │ │
│  │  ┌────────┐  ┌────────┐  ┌────────┐                 │ │
│  │  │App (K8s│  │App (K8s│  │App (K8s│                 │ │
│  │  │  Pod)  │  │  Pod)  │  │  Pod)  │                 │ │
│  │  └────────┘  └────────┘  └────────┘                 │ │
│  └──────────────────────────────────────────────────────┘ │
│                       │                                    │
│  ┌────────────────────┼─────────────────────────────────┐ │
│  │  ┌────────────────┐│  ┌─────────────────────────────┐│ │
│  │  │   RDS          ││  │   ElastiCache (Redis)      ││ │
│  │  │  (PostgreSQL)  ││  │   - Multi-AZ               ││ │
│  │  │  - Multi-AZ    ││  │   - Cluster mode           ││ │
│  │  │  - Read replica││  └─────────────────────────────┘│ │
│  │  └────────────────┘│                                 │ │
│  └──────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

---

## Summary

### Architectural Highlights

1. **Layered Architecture:** Clear separation of concerns (Controller → Service → Repository → Entity)
2. **Optimistic Locking:** Prevents seat conflicts using database version field
3. **Caching Strategy:** Redis cache-aside pattern with 2-second TTL
4. **Background Jobs:** Distributed scheduled tasks with ShedLock
5. **Stateless Design:** Enables horizontal scaling
6. **ACID Transactions:** PostgreSQL ensures data consistency
7. **Fault Tolerance:** Circuit breaker and retry patterns

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Optimistic Locking | Better performance than pessimistic locks for read-heavy workload |
| 2-second Cache TTL | Balance between performance (P95 < 1s) and data freshness |
| 120-second Hold | Business requirement for user decision time |
| ShedLock | Prevent duplicate job execution in multi-instance deployment |
| PostgreSQL | ACID compliance, mature ecosystem, excellent concurrency control |
| Redis | High-performance caching, sub-millisecond latency |

---

**Last Updated:** March 15, 2026  
**Maintained By:** SkyHigh Airlines Engineering Team

