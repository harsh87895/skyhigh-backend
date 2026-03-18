# Product Requirements Document (PRD)
# SkyHigh Core - Digital Check-In System

**Version:** 1.0  
**Date:** March 15, 2026  
**Project:** SkyHigh Airlines Digital Check-In Backend

---

## 1. Executive Summary

SkyHigh Airlines requires a robust, high-performance digital check-in system to handle heavy peak-hour traffic. During popular flight check-in windows, hundreds of passengers simultaneously select seats, add baggage, and complete check-in. The system must prevent seat conflicts, handle short-lived seat reservations, support baggage fee handling, and scale reliably during check-in rushes.

---

## 2. Problem Statement

### Current Challenges
- **Seat Conflicts:** Multiple passengers attempting to book the same seat simultaneously
- **Performance Issues:** Slow response times during peak check-in periods
- **Manual Processes:** Baggage validation and fee collection requires manual intervention
- **Scalability:** System cannot handle hundreds of concurrent users
- **Data Consistency:** Race conditions leading to double bookings

### Business Impact
- Customer dissatisfaction due to booking conflicts
- Revenue loss from manual baggage fee processing
- Operational inefficiency during peak hours
- Brand reputation damage from system failures

---

## 3. Goals & Objectives

### Primary Goals
1. **Eliminate Seat Conflicts:** Guarantee no double bookings under any concurrent load
2. **Fast Check-In:** Provide sub-second seat map access (P95 < 1s)
3. **Automated Baggage Handling:** Real-time weight validation and fee calculation
4. **Scalability:** Support 500+ concurrent users during peak times
5. **Reliability:** 99.9% uptime during check-in windows

### Success Metrics
- **Zero** seat conflicts in production
- **P95 latency < 1 second** for seat map retrieval
- **< 2% payment failure rate** for baggage fees
- **120-second seat hold** accuracy (±1 second)
- **99.9% system availability**

---

## 4. Target Users

### Primary Users
1. **Passengers**
   - Self-service check-in via mobile/web
   - Seat selection and confirmation
   - Baggage declaration and payment

2. **Airport Kiosks**
   - Automated check-in terminals
   - High-volume concurrent usage
   - Real-time seat availability

### Secondary Users
1. **System Administrators**
   - Monitor system health
   - Manage flight configurations
   - Handle exceptions

2. **Airline Operations**
   - View check-in statistics
   - Manage flight capacity

---

## 5. Functional Requirements

### FR-1: Seat Availability & Lifecycle Management

#### FR-1.1 Seat States
Seats must follow a strict state machine:
```
AVAILABLE → HELD → CONFIRMED
```

**States:**
- **AVAILABLE:** Seat can be viewed and selected by any passenger
- **HELD:** Seat temporarily reserved for exactly 120 seconds
- **CONFIRMED:** Seat permanently assigned (immutable)

**Rules:**
- Only AVAILABLE seats can transition to HELD
- Only HELD seats can transition to CONFIRMED
- CONFIRMED seats never change state
- HELD seats expire back to AVAILABLE after 120 seconds

#### FR-1.2 Concurrency Requirements
- **Critical:** Only ONE passenger can hold a seat at any given time
- System must use database-level locking (optimistic locking with version control)
- Race conditions must be prevented through transaction isolation

---

### FR-2: Time-Bound Seat Hold (120 Seconds)

#### FR-2.1 Hold Duration
- **Exact Duration:** 120 seconds (2 minutes)
- **Start Time:** Moment of successful hold request
- **Expiration:** Automatic release at T+120 seconds

#### FR-2.2 Hold Behavior
- During hold period:
  - Seat displays as "HELD" to other passengers
  - Only the holding passenger can confirm
  - No other passenger can hold or confirm
  
- After expiration:
  - Seat automatically returns to AVAILABLE
  - Hold record marked as EXPIRED
  - Seat becomes available for other passengers

#### FR-2.3 Background Job
- Scheduled job runs every 10 seconds
- Identifies holds where `expires_at < current_time`
- Updates seat status from HELD to AVAILABLE
- Updates hold status to EXPIRED
- Uses distributed locking to prevent duplicate execution

---

### FR-3: Conflict-Free Seat Assignment

#### FR-3.1 Concurrency Control
**Requirement:** If 100 passengers attempt to reserve the same seat simultaneously, exactly 1 succeeds.

**Implementation Requirements:**
- Database optimistic locking using version field
- Transaction isolation level: READ_COMMITTED or higher
- Retry mechanism with exponential backoff
- Clear error messages for failed attempts

#### FR-3.2 Edge Cases
1. **Double Hold Attempt:** Second request fails with `SEAT_UNAVAILABLE`
2. **Expired Hold Confirm:** Confirmation fails if hold has expired
3. **Network Retry:** Idempotency for hold/confirm operations
4. **Database Deadlock:** Automatic retry with backoff

---

### FR-4: Baggage Validation & Payment Pause

#### FR-4.1 Baggage Weight Limits
- **Free Limit:** 25kg per passenger
- **Excess Fee:** $10 per kg over limit
- **Maximum Weight:** None (all excess is chargeable)

#### FR-4.2 Check-In Flow with Baggage
1. Passenger starts check-in
2. Passenger declares baggage weight
3. System validates:
   - If weight ≤ 25kg → Continue to seat selection
   - If weight > 25kg → Pause for payment
4. System calculates excess fee: `(weight - 25) * 10`
5. Passenger completes payment
6. Check-in status updates to allow continuation

#### FR-4.3 Check-In States
```
INITIATED → AWAITING_PAYMENT → SEAT_SELECTED → COMPLETED
                ↓
            (if weight ≤ 25kg, skip payment)
```

**States:**
- **INITIATED:** Check-in started, no baggage declared
- **AWAITING_PAYMENT:** Excess baggage detected, payment required
- **SEAT_SELECTED:** Seat held/confirmed, awaiting final confirmation
- **COMPLETED:** Check-in fully completed

#### FR-4.4 Payment Integration
- Mock payment service endpoint
- Always succeeds for testing
- Real integration point for production payment gateway
- Circuit breaker pattern for payment service failures

---

### FR-5: High-Performance Seat Map Access

#### FR-5.1 Performance Requirements
- **Response Time:** P95 < 1 second
- **Concurrent Users:** Support 500+ simultaneous requests
- **Cache Strategy:** Redis cache with 2-second TTL
- **Data Freshness:** Near real-time (max 2s stale)

#### FR-5.2 Seat Map Data
**Response includes:**
- Seat number
- Seat class (ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST)
- Position (WINDOW, MIDDLE, AISLE)
- Status (AVAILABLE, HELD, CONFIRMED)
- Price

**Filtering:**
- By seat class
- By position preference
- Only show available/held seats (exclude confirmed for selection)

#### FR-5.3 Caching Strategy
- **Cache Key:** `flight:{flightId}:seatmap`
- **TTL:** 2 seconds
- **Cache Invalidation:** On seat status change (optional for real-time needs)
- **Cache Miss:** Fetch from database, populate cache

---

### FR-6: API Endpoints

#### Check-In Endpoints
```
POST /api/v1/check-in/start
GET  /api/v1/check-in/{checkInId}
PUT  /api/v1/check-in/{checkInId}/complete
```

#### Seat Endpoints
```
GET  /api/v1/flights/{flightId}/seats
POST /api/v1/seats/hold
POST /api/v1/seats/confirm
```

#### Baggage Endpoints
```
POST /api/v1/baggage/validate
GET  /api/v1/baggage/{checkInId}
```

#### Payment Endpoints
```
POST /api/v1/payment/process
```

---

## 6. Non-Functional Requirements (NFRs)

### NFR-1: Performance
- **Response Time:** 
  - Seat map retrieval: P95 < 1s, P99 < 2s
  - Seat hold/confirm: P95 < 500ms
  - Check-in start: P95 < 300ms
- **Throughput:** 
  - 500+ concurrent users
  - 1000+ requests per minute during peak

### NFR-2: Scalability
- **Horizontal Scaling:** Stateless application design
- **Database:** Connection pooling (20 max connections)
- **Cache:** Redis with connection pooling
- **Load Balancing:** Support multiple app instances

### NFR-3: Reliability
- **Availability:** 99.9% uptime during check-in windows
- **Data Consistency:** ACID transactions for critical operations
- **Fault Tolerance:** 
  - Circuit breaker for external services
  - Retry logic with exponential backoff
  - Graceful degradation

### NFR-4: Security
- **Input Validation:** All request bodies validated
- **SQL Injection:** Use parameterized queries
- **Rate Limiting:** Prevent abuse (future enhancement)
- **Authentication:** API key/JWT (future enhancement)

### NFR-5: Observability
- **Logging:** Structured logging with log levels
- **Metrics:** 
  - Request count, latency, error rate
  - Cache hit/miss ratio
  - Database connection pool usage
- **Health Checks:** 
  - Application health endpoint
  - Database connectivity
  - Redis connectivity

### NFR-6: Maintainability
- **Code Quality:** 
  - Clean code principles
  - SOLID design patterns
  - Comprehensive comments
- **Testing:** 
  - Unit test coverage > 70%
  - Integration tests for critical flows
  - Load testing for performance validation
- **Documentation:** 
  - API documentation (OpenAPI/Swagger)
  - Architecture diagrams
  - Deployment guides

### NFR-7: Data Integrity
- **Optimistic Locking:** Version field on seats table
- **Transaction Isolation:** READ_COMMITTED
- **Idempotency:** Hold/confirm operations idempotent
- **Audit Trail:** Created/updated timestamps on all records

---

## 7. Edge Cases & Scenarios

### Scenario 1: Concurrent Seat Selection
**Given:** 100 passengers select the same seat simultaneously  
**Expected:** Exactly 1 succeeds, 99 receive "Seat Unavailable" error  
**Mechanism:** Optimistic locking on seats table

### Scenario 2: Hold Expiration During Confirmation
**Given:** Passenger holds seat, wait 121 seconds, then confirms  
**Expected:** Confirmation fails with "Hold Expired" error  
**Mechanism:** Background job expires hold at 120s

### Scenario 3: Payment Service Failure
**Given:** Passenger has excess baggage, payment service is down  
**Expected:** Check-in paused, error message shown, circuit breaker opens  
**Mechanism:** Resilience4j circuit breaker pattern

### Scenario 4: Database Connection Loss
**Given:** Database connection drops during transaction  
**Expected:** Transaction rolled back, error returned to user  
**Mechanism:** Spring transaction management

### Scenario 5: Cache Miss During Peak Load
**Given:** 1000 concurrent requests, cache expired  
**Expected:** Database queried, cache populated, subsequent requests served from cache  
**Mechanism:** Cache-aside pattern with Redis

### Scenario 6: Duplicate Hold Request
**Given:** Network issue causes duplicate hold request  
**Expected:** First request succeeds, second is idempotent (returns existing hold)  
**Mechanism:** Unique constraint on seat_holds or idempotency check

### Scenario 7: Seat Hold Abandonment
**Given:** Passenger holds seat but never confirms  
**Expected:** After 120s, seat released automatically  
**Mechanism:** Scheduled job with ShedLock

---

## 8. Assumptions & Constraints

### Assumptions
1. Each passenger can have only ONE active check-in per flight
2. Seat numbers are unique within a flight
3. Flight data is pre-populated (not managed by this service)
4. Payment service response time < 5 seconds
5. Database supports optimistic locking (PostgreSQL)

### Constraints
1. Hold duration is fixed at 120 seconds (not configurable per request)
2. Baggage fee calculation is simple (no tiers or discounts)
3. No partial check-in (must complete entire flow)
4. Seat map cache TTL is 2 seconds (trade-off between performance and accuracy)
5. Single payment per check-in (no split payments)

---

## 9. Out of Scope

The following are explicitly **NOT** included in this release:

1. **User Authentication/Authorization:** No JWT, OAuth, or user management
2. **Booking Creation:** Assumes bookings exist before check-in
3. **Boarding Pass Generation:** Not generating PDF/mobile boarding passes
4. **Notification System:** No email/SMS notifications
5. **Multi-language Support:** English only
6. **Advanced Analytics:** No dashboards or reporting
7. **Rate Limiting:** Not implementing API rate limits
8. **Admin UI:** No web interface for administrators
9. **Real Payment Gateway:** Using mock payment service only
10. **Seat Map Visualization:** No graphical seat map rendering

---

## 10. Success Criteria

### Launch Criteria
- ✅ All functional requirements implemented
- ✅ NFRs validated through load testing
- ✅ Zero seat conflicts in stress testing
- ✅ API documentation complete
- ✅ Docker deployment working
- ✅ Database migrations tested

### Post-Launch Monitoring
- Monitor seat conflict rate (target: 0%)
- Track P95 latency for seat map (target: < 1s)
- Measure check-in completion rate (target: > 95%)
- Monitor payment success rate (target: > 98%)
- Track background job execution (target: 100% success)

---

## 11. Dependencies

### External Systems
1. **PostgreSQL Database:** Version 15+
2. **Redis Cache:** Version 7+
3. **Payment Service:** Mock service (future: real gateway)

### Technology Stack
- **Framework:** Spring Boot 3.2.3
- **Language:** Java 17
- **Build Tool:** Maven
- **Database:** PostgreSQL 15
- **Cache:** Redis 7
- **ORM:** Hibernate/JPA
- **Migration:** Flyway
- **Scheduler:** Spring Scheduler + ShedLock
- **Resilience:** Resilience4j

---

## 12. Timeline & Milestones

### Phase 1: Foundation (Completed)
- ✅ Database schema design
- ✅ Entity models and repositories
- ✅ Basic CRUD operations

### Phase 2: Core Features (Completed)
- ✅ Seat hold/confirm logic
- ✅ Optimistic locking implementation
- ✅ Background job for hold expiration
- ✅ Baggage validation and payment pause

### Phase 3: Performance (Completed)
- ✅ Redis caching for seat maps
- ✅ Connection pooling optimization
- ✅ API endpoint optimization

### Phase 4: Deployment (Completed)
- ✅ Docker containerization
- ✅ Docker Compose setup
- ✅ API documentation

---

## 13. Glossary

- **Check-In:** Process of confirming attendance on a flight and selecting a seat
- **Seat Hold:** Temporary 120-second reservation of a seat
- **Optimistic Locking:** Concurrency control using version numbers
- **Circuit Breaker:** Fault tolerance pattern to prevent cascade failures
- **TTL:** Time To Live - cache expiration duration
- **P95 Latency:** 95th percentile response time
- **ACID:** Atomicity, Consistency, Isolation, Durability

---

**Document Owner:** SkyHigh Airlines Engineering Team  
**Last Updated:** March 15, 2026  
**Status:** Approved for Implementation

