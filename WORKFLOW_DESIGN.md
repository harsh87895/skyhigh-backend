# Workflow Design Document

## Overview

This document explains the implementation workflows and flows for the SkyHigh Core Digital Check-In System. It includes detailed flow diagrams and sequence diagrams for primary use cases.

---

## Table of Contents

1. [Complete Check-In Flow](#1-complete-check-in-flow)
2. [Seat Hold Flow](#2-seat-hold-flow)
3. [Seat Confirmation Flow](#3-seat-confirmation-flow)
4. [Baggage Validation Flow](#4-baggage-validation-flow)
5. [Seat Hold Expiration Flow](#5-seat-hold-expiration-flow)
6. [Concurrent Seat Selection Flow](#6-concurrent-seat-selection-flow)
7. [Database State Transitions](#7-database-state-transitions)

---

## 1. Complete Check-In Flow

### Overview
This is the end-to-end passenger check-in flow from initiation to completion.

### Flow Diagram

```
┌──────────┐
│ Passenger│
└─────┬────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. START CHECK-IN                                           │
│    POST /api/v1/check-in/start                              │
│    { passengerId, flightId, bookingReference }              │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ CheckInService.startCheckIn()                               │
│ - Validate passenger exists                                 │
│ - Validate flight exists                                    │
│ - Check for duplicate check-in                              │
│ - Create CheckIn record with status=INITIATED               │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Response: checkInId, status=INITIATED                       │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. DECLARE BAGGAGE                                          │
│    POST /api/v1/baggage/validate                            │
│    { checkInId, weight, pieces }                            │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
      ┌──────────────┐
      │ weight > 25kg?│
      └──┬────────┬───┘
         │ YES    │ NO
         │        │
         ▼        ▼
    ┌────────┐   ┌─────────────────────────┐
    │ PAYMENT│   │ 3. BROWSE SEAT MAP      │
    │ REQUIRED│  │    GET /api/v1/flights/ │
    └───┬────┘   │    {flightId}/seats     │
        │        └─────┬───────────────────┘
        │              │
        ▼              │
    ┌─────────────────────────┐  │
    │ BaggageService          │  │
    │ - Calculate excess fee  │  │
    │ - Set paymentRequired   │  │
    │ - Status=AWAITING_PAYMENT│ │
    └───┬─────────────────────┘  │
        │                        │
        ▼                        │
    ┌────────────────────┐       │
    │ POST /api/v1/      │       │
    │ payment/process    │       │
    │ { checkInId, amt } │       │
    └───┬────────────────┘       │
        │                        │
        ▼                        │
    ┌─────────────────┐          │
    │ PaymentService  │          │
    │ - Process payment│         │
    │ - Update status │          │
    └───┬─────────────┘          │
        │                        │
        └──────────┬─────────────┘
                   │
                   ▼
    ┌─────────────────────────────────────────┐
    │ 3. BROWSE SEAT MAP (Cached)             │
    │    SeatService.getSeatMap()             │
    │    - Check Redis cache (TTL=2s)         │
    │    - If miss, query DB and cache        │
    │    - Filter by class/position           │
    │    - Return available/held seats        │
    └─────┬───────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ Passenger selects seat                   │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ 4. HOLD SEAT (120 seconds)               │
    │    POST /api/v1/seats/hold               │
    │    { seatId, checkInId }                 │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ SeatHoldService.holdSeat()               │
    │ - Lock seat with optimistic locking      │
    │ - Update seat.status = HELD              │
    │ - Create SeatHold record                 │
    │ - Set expiresAt = now + 120s             │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ Response: holdId, expiresAt              │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ 5. CONFIRM SEAT                          │
    │    POST /api/v1/seats/confirm            │
    │    { holdId }                            │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ SeatHoldService.confirmSeat()            │
    │ - Validate hold not expired              │
    │ - Update seat.status = CONFIRMED         │
    │ - Update hold.status = CONFIRMED         │
    │ - Update checkIn.seatNumber              │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ 6. COMPLETE CHECK-IN                     │
    │    PUT /api/v1/check-in/{id}/complete    │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ CheckInService.completeCheckIn()         │
    │ - Update status = COMPLETED              │
    │ - Set completedAt timestamp              │
    └─────┬────────────────────────────────────┘
          │
          ▼
    ┌──────────────────────────────────────────┐
    │ Success: Check-in complete ✓             │
    └──────────────────────────────────────────┘
```

### State Transitions

```
CheckIn Status Flow:
INITIATED → AWAITING_PAYMENT → SEAT_SELECTED → COMPLETED
              ↓ (if no baggage or weight ≤ 25kg)
              └─────────────────────┘
```

---

## 2. Seat Hold Flow

### Detailed Sequence Diagram

```
Passenger    Controller    SeatHoldService    SeatRepository    SeatHoldRepository    Database
    │            │                │                 │                   │                │
    │─POST /api/v1/seats/hold────▶│                 │                   │                │
    │  {seatId, checkInId}        │                 │                   │                │
    │            │                │                 │                   │                │
    │            │─holdSeat()────▶│                 │                   │                │
    │            │                │                 │                   │                │
    │            │                │─findBySeatIdAndStatus(seatId, AVAILABLE)─────────────▶│
    │            │                │                 │                   │                │
    │            │                │◀────────────────────────────────────────────────seat─┤
    │            │                │                 │                   │                │
    │            │                │                 │                   │                │
    │            │                ├─Check seat.status == AVAILABLE?                      │
    │            │                │  If NO: throw SeatUnavailableException                │
    │            │                │                 │                   │                │
    │            │                │─seat.setStatus(HELD)                │                │
    │            │                │─seat.version++  │                   │                │
    │            │                │                 │                   │                │
    │            │                │─save(seat)─────▶│                   │                │
    │            │                │                 │─UPDATE seats───────────────────────▶│
    │            │                │                 │  WHERE id=? AND version=old        │
    │            │                │                 │                   │                │
    │            │                │                 │                   │  ┌──────────┐  │
    │            │                │                 │                   │  │If rows=0:│  │
    │            │                │                 │                   │  │ throw    │  │
    │            │                │                 │                   │  │Optimistic│  │
    │            │                │                 │                   │  │LockExcept│  │
    │            │                │                 │                   │  └──────────┘  │
    │            │                │                 │                   │                │
    │            │                │─Create SeatHold:│                   │                │
    │            │                │  holdId = UUID  │                   │                │
    │            │                │  seatId = seat.id                   │                │
    │            │                │  checkInId      │                   │                │
    │            │                │  status = HELD  │                   │                │
    │            │                │  heldAt = now   │                   │                │
    │            │                │  expiresAt = now + 120s             │                │
    │            │                │                 │                   │                │
    │            │                │─save(hold)──────────────────────────▶│                │
    │            │                │                 │                   │─INSERT hold───▶│
    │            │                │                 │                   │                │
    │            │                │◀────────────────────────────────────hold saved─────┤
    │            │                │                 │                   │                │
    │            │◀─HoldResponse──┤                 │                   │                │
    │            │  {holdId, seatNumber, expiresAt} │                   │                │
    │            │                │                 │                   │                │
    │◀200 OK─────┤                │                 │                   │                │
    │ HoldResponse                │                 │                   │                │
    │            │                │                 │                   │                │
```

### Business Rules

1. **Optimistic Locking:** Version field prevents concurrent holds
2. **Hold Duration:** Exactly 120 seconds from `heldAt`
3. **Expiration Calculation:** `expiresAt = Instant.now().plusSeconds(120)`
4. **Status Transition:** `AVAILABLE → HELD` only

### Error Scenarios

| Scenario | Exception | HTTP Status |
|----------|-----------|-------------|
| Seat already held | SeatUnavailableException | 409 CONFLICT |
| Seat already confirmed | SeatUnavailableException | 409 CONFLICT |
| Concurrent hold attempt | OptimisticLockException | 409 CONFLICT |
| Seat not found | SeatNotFoundException | 404 NOT FOUND |

---

## 3. Seat Confirmation Flow

### Sequence Diagram

```
Passenger    Controller    SeatHoldService    SeatRepository    SeatHoldRepository
    │            │                │                 │                   │
    │─POST /api/v1/seats/confirm─▶│                 │                   │
    │  {holdId}                   │                 │                   │
    │            │                │                 │                   │
    │            │─confirmSeat()─▶│                 │                   │
    │            │                │                 │                   │
    │            │                │─findById(holdId)────────────────────▶│
    │            │                │                 │                   │
    │            │                │◀────────────────────────────────hold─┤
    │            │                │                 │                   │
    │            │                │                 │                   │
    │            │                ├─Validate:       │                   │
    │            │                │  1. hold.status == HELD?             │
    │            │                │  2. hold.expiresAt > now?            │
    │            │                │     If NO: throw HoldExpiredException│
    │            │                │                 │                   │
    │            │                │─findById(seatId)▶                   │
    │            │                │                 │                   │
    │            │                │◀────────────seat─┤                   │
    │            │                │                 │                   │
    │            │                │─seat.setStatus(CONFIRMED)           │
    │            │                │─seat.version++  │                   │
    │            │                │                 │                   │
    │            │                │─save(seat)─────▶│                   │
    │            │                │                 │                   │
    │            │                │─hold.setStatus(CONFIRMED)           │
    │            │                │─hold.confirmedAt = now              │
    │            │                │                 │                   │
    │            │                │─save(hold)──────────────────────────▶│
    │            │                │                 │                   │
    │            │◀─ConfirmResponse                 │                   │
    │            │  {seatNumber, confirmedAt}       │                   │
    │            │                │                 │                   │
    │◀200 OK─────┤                │                 │                   │
    │ ConfirmResponse             │                 │                   │
    │            │                │                 │                   │
```

### Validation Rules

1. **Hold Must Exist:** `holdId` must reference valid SeatHold record
2. **Hold Not Expired:** `Instant.now() < hold.expiresAt`
3. **Hold Status:** Must be `HELD` (not already confirmed or expired)
4. **Seat Status:** Must still be `HELD` (not changed by background job)

### State Transitions

```
Seat:     HELD → CONFIRMED (permanent, immutable)
SeatHold: HELD → CONFIRMED
```

---

## 4. Baggage Validation Flow

### Flow Diagram

```
Passenger    Controller    BaggageService    CheckInRepository    BaggageRepository
    │            │                │                   │                   │
    │─POST /api/v1/baggage/validate──▶│                 │                 │
    │  {checkInId, weight, pieces}    │                 │                 │
    │            │                │                   │                   │
    │            │─validateBaggage()─▶│                 │                 │
    │            │                │                   │                   │
    │            │                │─findById(checkInId)▶                  │
    │            │                │                   │                   │
    │            │                │◀─────────checkIn──┤                   │
    │            │                │                   │                   │
    │            │                ├─Calculate:        │                   │
    │            │                │  maxFreeWeight = 25kg                 │
    │            │                │  if weight > 25:  │                   │
    │            │                │    excessWeight = weight - 25         │
    │            │                │    excessFee = excessWeight * $10     │
    │            │                │    paymentRequired = true             │
    │            │                │  else:            │                   │
    │            │                │    excessWeight = 0                   │
    │            │                │    excessFee = 0  │                   │
    │            │                │    paymentRequired = false            │
    │            │                │                   │                   │
    │            │                │─Create Baggage:   │                   │
    │            │                │  baggageId = UUID │                   │
    │            │                │  checkInId        │                   │
    │            │                │  weight, pieces   │                   │
    │            │                │  excessWeight, excessFee              │
    │            │                │  validatedAt = now│                   │
    │            │                │                   │                   │
    │            │                │─save(baggage)─────────────────────────▶│
    │            │                │                   │                   │
    │            │                │─Update CheckIn:   │                   │
    │            │                │  if paymentRequired:                  │
    │            │                │    status = AWAITING_PAYMENT          │
    │            │                │    paymentRequired = true             │
    │            │                │                   │                   │
    │            │                │─save(checkIn)────▶│                   │
    │            │                │                   │                   │
    │            │◀─BaggageResponse                   │                   │
    │            │  {baggageId, excessWeight, excessFee, paymentRequired} │
    │            │                │                   │                   │
    │◀200 OK─────┤                │                   │                   │
    │ BaggageResponse             │                   │                   │
    │            │                │                   │                   │
    │            │                │                   │                   │
    │ if paymentRequired:         │                   │                   │
    │─POST /api/v1/payment/process──────────────────────────────────────▶│
    │  {checkInId, amount}        │                   │                   │
    │            │                │                   │                   │
```

### Fee Calculation Logic

```java
BigDecimal MAX_FREE_WEIGHT = 25.0;  // kg
BigDecimal FEE_PER_KG = 10.0;       // USD

if (weight > MAX_FREE_WEIGHT) {
    excessWeight = weight - MAX_FREE_WEIGHT;
    excessFee = excessWeight * FEE_PER_KG;
    checkIn.setPaymentRequired(true);
    checkIn.setStatus(AWAITING_PAYMENT);
} else {
    excessWeight = 0;
    excessFee = 0;
    checkIn.setPaymentRequired(false);
    // Proceed to seat selection
}
```

### Example Calculations

| Weight | Excess Weight | Excess Fee | Payment Required |
|--------|---------------|------------|------------------|
| 20 kg  | 0 kg          | $0         | No               |
| 25 kg  | 0 kg          | $0         | No               |
| 30 kg  | 5 kg          | $50        | Yes              |
| 40 kg  | 15 kg         | $150       | Yes              |

---

## 5. Seat Hold Expiration Flow

### Background Job Flow

```
┌─────────────────────────────────────────────────────────────┐
│ SCHEDULED JOB (Every 10 seconds)                            │
│ @Scheduled(fixedRateString = "10000")                       │
│ @SchedulerLock(name = "SeatHoldExpirationJob")             │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ ShedLock acquires distributed lock                          │
│ - Only ONE instance runs across cluster                     │
│ - Lock timeout: 9 seconds                                   │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│ Query: Find expired holds                                   │
│ SELECT * FROM seat_holds                                    │
│ WHERE status = 'HELD'                                       │
│   AND expires_at < NOW()                                    │
└─────┬───────────────────────────────────────────────────────┘
      │
      ▼
      ┌──────────────┐
      │ Any expired? │
      └──┬────────┬──┘
         │ NO     │ YES
         │        │
         ▼        ▼
    ┌────────┐   ┌───────────────────────────────────────┐
    │  END   │   │ For each expired hold:                │
    └────────┘   │                                       │
                 │ 1. Load Seat entity                   │
                 │ 2. seat.setStatus(AVAILABLE)          │
                 │ 3. seat.version++                     │
                 │ 4. seatRepository.save(seat)          │
                 │                                       │
                 │ 5. hold.setStatus(EXPIRED)            │
                 │ 6. seatHoldRepository.save(hold)      │
                 │                                       │
                 │ 7. Log expiration                     │
                 │    "Expired hold {holdId} for         │
                 │     seat {seatNumber}"                │
                 └───────────────────────────────────────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │ Release ShedLock│
                         └─────────────────┘
```

### Detailed Sequence Diagram

```
Scheduler    ShedLock    SeatHoldExpirationJob    SeatHoldRepo    SeatRepo    Database
    │            │                │                     │            │            │
    ├─Every 10s──▶│                │                     │            │            │
    │            │                │                     │            │            │
    │            │─Acquire Lock──▶│                     │            │            │
    │            │  (JDBC-based)  │                     │            │            │
    │            │                │                     │            │            │
    │            │◀Lock Acquired──┤                     │            │            │
    │            │  (or skip)     │                     │            │            │
    │            │                │                     │            │            │
    │            │                │─expireHolds()       │            │            │
    │            │                │                     │            │            │
    │            │                │─findExpiredHolds()─▶│            │            │
    │            │                │  (expiresAt < now)  │            │            │
    │            │                │                     │            │            │
    │            │                │◀───List<SeatHold>───┤            │            │
    │            │                │                     │            │            │
    │            │                │─For each hold:      │            │            │
    │            │                │                     │            │            │
    │            │                │─findById(seatId)────────────────▶│            │
    │            │                │                     │            │            │
    │            │                │◀─────────────────────────────seat┤            │
    │            │                │                     │            │            │
    │            │                │─seat.setStatus(AVAILABLE)        │            │
    │            │                │─seat.version++      │            │            │
    │            │                │                     │            │            │
    │            │                │─save(seat)──────────────────────▶│            │
    │            │                │                     │            │─UPDATE────▶│
    │            │                │                     │            │            │
    │            │                │─hold.setStatus(EXPIRED)          │            │
    │            │                │                     │            │            │
    │            │                │─save(hold)─────────▶│            │            │
    │            │                │                     │─UPDATE────────────────▶│
    │            │                │                     │            │            │
    │            │                │─log.info("Expired hold {} ...")  │            │
    │            │                │                     │            │            │
    │            │                │◀────────────────────┴────────────┴────────────┤
    │            │                │                     │            │            │
    │            │◀Release Lock───┤                     │            │            │
    │            │                │                     │            │            │
```

### ShedLock Configuration

```java
@SchedulerLock(
    name = "SeatHoldExpirationJob",
    lockAtLeastFor = "5s",   // Minimum lock duration
    lockAtMostFor = "9s"     // Maximum lock duration (timeout)
)
```

**Guarantees:**
- Only ONE instance executes the job at a time
- If instance crashes, lock auto-releases after 9 seconds
- Next execution can start after 5 seconds minimum

---

## 6. Concurrent Seat Selection Flow

### Scenario: Two Passengers Select Same Seat

```
Passenger A             Database             Passenger B
    │                       │                       │
    │─holdSeat(12A)────────▶│                       │
    │  version=0            │                       │
    │                       │◀────holdSeat(12A)─────┤
    │                       │       version=0       │
    │                       │                       │
    │                  ┌────┴────┐                  │
    │                  │ RACE!   │                  │
    │                  │         │                  │
    │                  │ Lock    │                  │
    │                  │ Seat 12A│                  │
    │                  └────┬────┘                  │
    │                       │                       │
    │◀─────────────────────┤                       │
    │  SUCCESS!             │                       │
    │  - seat.status = HELD │                       │
    │  - seat.version = 1   │                       │
    │  - hold created       │                       │
    │  Response: holdId     │                       │
    │                       │                       │
    │                       ├──────────────────────▶│
    │                       │                  FAIL!│
    │                       │  OptimisticLockException
    │                       │  (version mismatch)   │
    │                       │                       │
    │                       │  Retry with version=1?│
    │                       │  ──▶ Seat now HELD    │
    │                       │  ──▶ SeatUnavailable  │
    │                       │                       │
    │                       │  Response: 409 CONFLICT
    │                       │  "Seat already held"  │
    │                       │                       │
```

### Optimistic Locking Mechanism

**SQL Update:**
```sql
UPDATE seats
SET status = 'HELD', version = version + 1
WHERE seat_id = ?
  AND version = ?  -- ← This ensures no concurrent modification
```

**Result:**
- If `version` matches: Update succeeds, returns 1 row
- If `version` doesn't match: Update fails, returns 0 rows → `OptimisticLockException`

### Retry Strategy

```java
@Retry(name = "seatHold", maxAttempts = 3)
public SeatHold holdSeat(UUID seatId, UUID checkInId) {
    // Optimistic locking attempt
    // If OptimisticLockException → retry
    // If still fails → return SeatUnavailableException
}
```

**Configuration:**
```yaml
resilience4j:
  retry:
    instances:
      seatHold:
        maxAttempts: 3
        waitDuration: 100ms
        exponentialBackoffMultiplier: 2
```

---

## 7. Database State Transitions

### Seat Status State Machine

```
┌───────────┐
│ AVAILABLE │ ◀───────────────────────────┐
└─────┬─────┘                             │
      │                                   │
      │ holdSeat()                        │ expireHold()
      │ (optimistic lock)                 │ (background job)
      │                                   │
      ▼                                   │
┌───────────┐                             │
│   HELD    │ ────────────────────────────┘
└─────┬─────┘    (if not confirmed
      │           within 120s)
      │
      │ confirmSeat()
      │ (before expiration)
      │
      ▼
┌───────────┐
│ CONFIRMED │ (terminal state - immutable)
└───────────┘
```

**Rules:**
1. **AVAILABLE → HELD:** Only via `holdSeat()` with optimistic lock
2. **HELD → CONFIRMED:** Only via `confirmSeat()` within 120 seconds
3. **HELD → AVAILABLE:** Automatic after 120 seconds (background job)
4. **CONFIRMED → (any):** NEVER (immutable)

---

### Check-In Status State Machine

```
┌───────────┐
│ INITIATED │
└─────┬─────┘
      │
      │ validateBaggage()
      │
      ├────────────┬────────────┐
      │ weight>25kg│ weight≤25kg│
      ▼            ▼            │
┌─────────────┐                 │
│  AWAITING_  │                 │
│  PAYMENT    │                 │
└──────┬──────┘                 │
       │                        │
       │ processPayment()       │
       │                        │
       └────────────┬───────────┘
                    │
                    ▼
              ┌─────────────┐
              │    SEAT_    │
              │  SELECTED   │
              └──────┬──────┘
                     │
                     │ completeCheckIn()
                     │
                     ▼
              ┌─────────────┐
              │  COMPLETED  │ (terminal)
              └─────────────┘
```

---

### SeatHold Status State Machine

```
┌───────────┐
│   HELD    │ ◀─── Created with expiresAt
└─────┬─────┘
      │
      ├──────────────┬───────────────┐
      │ confirmSeat()│ 120s elapsed  │
      │ (in time)    │ (expiration)  │
      │              │               │
      ▼              ▼               │
┌───────────┐  ┌───────────┐        │
│ CONFIRMED │  │  EXPIRED  │        │
└───────────┘  └───────────┘        │
  (terminal)     (terminal)         │
```

---

## Summary

### Key Workflows

1. **Complete Check-In:** 6-step process from initiation to completion
2. **Seat Hold:** Optimistic locking with 120-second timer
3. **Seat Confirmation:** Validation and permanent assignment
4. **Baggage Validation:** Weight check and fee calculation
5. **Hold Expiration:** Background job with distributed locking
6. **Concurrent Selection:** Optimistic locking prevents conflicts

### Critical Design Patterns

1. **Optimistic Locking:** Prevents race conditions
2. **State Machine:** Clear transitions with validation
3. **Background Jobs:** Automatic cleanup with ShedLock
4. **Circuit Breaker:** Fault tolerance for external services
5. **Retry Logic:** Exponential backoff for transient failures
6. **Caching:** Redis for high-performance seat maps

---

**Last Updated:** March 15, 2026  
**Maintained By:** SkyHigh Airlines Engineering Team

