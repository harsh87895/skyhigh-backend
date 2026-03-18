# SkyHigh Core - Digital Check-In System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

## 📋 Overview

**SkyHigh Core** is a high-performance, distributed digital check-in system designed for SkyHigh Airlines to handle heavy peak-hour traffic during flight check-in windows. The system manages seat reservations, baggage validation, and payment processing with a focus on **preventing seat conflicts** and **delivering fast, reliable service** to hundreds of concurrent users.

### Key Features

✅ **Conflict-Free Seat Assignment** - Optimistic locking prevents double bookings  
✅ **Time-Bound Seat Holds** - 120-second temporary reservations  
✅ **High-Performance Seat Maps** - Redis caching (P95 < 1s)  
✅ **Automated Baggage Validation** - Real-time weight checks and fee calculation  
✅ **Scalable Architecture** - Supports 500+ concurrent users  
✅ **Background Processing** - Automatic hold expiration with distributed locking  
✅ **Fault Tolerance** - Circuit breakers and retry mechanisms  

---

## 🏗️ Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌──────────────┐
│   Client    │─────▶│  Spring Boot App │─────▶│  PostgreSQL  │
│ (Web/Mobile)│      │   (REST API)     │      │   Database   │
└─────────────┘      └──────────────────┘      └──────────────┘
                              │
                              ├─────────────▶ ┌──────────────┐
                              │               │    Redis     │
                              │               │    Cache     │
                              │               └──────────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │ Background Jobs  │
                     │ (Hold Expiration)│
                     └──────────────────┘
```

### Technology Stack

- **Backend Framework:** Spring Boot 3.2.3
- **Language:** Java 17
- **Database:** PostgreSQL 15 (with Flyway migrations)
- **Cache:** Redis 7 (for seat map caching)
- **Build Tool:** Maven
- **ORM:** Hibernate/JPA with optimistic locking
- **Scheduling:** Spring Scheduler + ShedLock (distributed lock)
- **Resilience:** Resilience4j (circuit breaker, retry)
- **API Documentation:** Swagger/OpenAPI
- **Containerization:** Docker + Docker Compose

---

## 🚀 Quick Start

### Prerequisites

Ensure you have the following installed:

- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/get-started)
- **Git** - [Download](https://git-scm.com/)

### Option 1: Run with Docker (Recommended)

This is the **easiest way** to get started. Docker Compose will start all required services.

```bash
# Clone the repository
git clone <repository-url>
cd skyhigh-backend

# Start all services (PostgreSQL, Redis, Application)
docker-compose up --build

# The application will be available at:
# http://localhost:8080
```

**Services Started:**
- **Application:** http://localhost:8080
- **PostgreSQL:** localhost:5432
- **Redis:** localhost:6379
- **API Documentation:** http://localhost:8080/swagger-ui.html

To stop all services:
```bash
docker-compose down
```

To stop and remove all data:
```bash
docker-compose down -v
```

---

### Option 2: Run Locally (Manual Setup)

If you prefer to run the application locally without Docker:

#### Step 1: Start PostgreSQL Database

```bash
# Using Docker
docker run --name skyhigh-postgres \
  -e POSTGRES_DB=skyhigh_db \
  -e POSTGRES_USER=skyhigh_user \
  -e POSTGRES_PASSWORD=skyhigh_pass \
  -p 5432:5432 \
  -d postgres:15-alpine

# Or install PostgreSQL locally and create database:
psql -U postgres
CREATE DATABASE skyhigh_db;
CREATE USER skyhigh_user WITH PASSWORD 'skyhigh_pass';
GRANT ALL PRIVILEGES ON DATABASE skyhigh_db TO skyhigh_user;
```

#### Step 2: Start Redis Cache

```bash
# Using Docker
docker run --name skyhigh-redis \
  -p 6379:6379 \
  -d redis:7-alpine

# Or install Redis locally:
# Windows: Download from https://redis.io/download
# Linux: sudo apt-get install redis-server
# macOS: brew install redis
redis-server
```

#### Step 3: Build and Run Application

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/skyhigh-core-1.0.0-SNAPSHOT.jar
```

The application will start on **http://localhost:8080**

---

## 🗄️ Database Setup

### Automatic Migration (Flyway)

The application uses **Flyway** for database migrations. Migrations run automatically on startup.

**Migration Scripts Location:** `src/main/resources/db/migration/`

- **V1__Initial_Schema.sql** - Creates tables (passengers, flights, seats, check_ins, seat_holds, baggage)
- **V2__Add_ShedLock.sql** - Adds distributed locking table for scheduled jobs
- **V3__Sample_Data.sql** - Inserts sample flight and passenger data for testing

### Manual Migration (if needed)

```bash
mvn flyway:migrate
```

### Database Schema

**Key Tables:**
- `passengers` - Passenger information
- `flights` - Flight details
- `seats` - Seat inventory with optimistic locking (version field)
- `check_ins` - Check-in sessions
- `seat_holds` - 120-second seat reservations
- `baggage` - Baggage declarations and fees

**See:** `docs/ARCHITECTURE.md` for detailed schema diagram

---

## 🔧 Configuration

### Application Configuration

Configuration is managed in `src/main/resources/application.yml`

**Key Properties:**

```yaml
# Seat Hold Configuration
app:
  seat-hold:
    duration-seconds: 120        # 2-minute hold period
    expiration-job-rate: 10000   # Run cleanup every 10 seconds

# Baggage Configuration
app:
  baggage:
    max-free-weight: 25.0        # 25kg free allowance
    fee-per-kg: 10.0             # $10 per kg over limit

# Redis Cache
spring:
  cache:
    redis:
      time-to-live: 2000         # 2-second cache TTL

# Database Connection Pool
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Environment Variables (Docker)

Override configuration using environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/skyhigh_db
SPRING_DATASOURCE_USERNAME=skyhigh_user
SPRING_DATASOURCE_PASSWORD=skyhigh_pass
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
```

---

## 📡 API Endpoints

### Swagger UI (Interactive Documentation)

Access the interactive API documentation:

**URL:** http://localhost:8080/swagger-ui.html

### Core Endpoints

#### 1. Check-In Management

```http
POST /api/v1/check-in/start
```
Start a new check-in session for a passenger.

**Request Body:**
```json
{
  "passengerId": "P12345",
  "flightId": "FL001",
  "bookingReference": "ABC123"
}
```

**Response:**
```json
{
  "checkInId": "550e8400-e29b-41d4-a716-446655440000",
  "passengerId": "P12345",
  "flightId": "FL001",
  "status": "INITIATED",
  "createdAt": "2026-03-15T10:00:00Z"
}
```

---

#### 2. Seat Map Browsing

```http
GET /api/v1/flights/{flightId}/seats
```
Get seat availability for a flight (cached for 2 seconds).

**Query Parameters:**
- `seatClass` (optional): ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST
- `position` (optional): WINDOW, MIDDLE, AISLE

**Response:**
```json
{
  "flightId": "FL001",
  "totalSeats": 180,
  "availableSeats": 45,
  "seats": [
    {
      "seatId": "550e8400-e29b-41d4-a716-446655440001",
      "seatNumber": "12A",
      "seatClass": "ECONOMY",
      "position": "WINDOW",
      "status": "AVAILABLE",
      "price": 50.00
    }
  ]
}
```

---

#### 3. Seat Hold (120-second reservation)

```http
POST /api/v1/seats/hold
```
Hold a seat for 120 seconds.

**Request Body:**
```json
{
  "seatId": "550e8400-e29b-41d4-a716-446655440001",
  "checkInId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "holdId": "550e8400-e29b-41d4-a716-446655440002",
  "seatNumber": "12A",
  "status": "HELD",
  "heldAt": "2026-03-15T10:05:00Z",
  "expiresAt": "2026-03-15T10:07:00Z"
}
```

---

#### 4. Seat Confirmation

```http
POST /api/v1/seats/confirm
```
Confirm a held seat (permanent assignment).

**Request Body:**
```json
{
  "holdId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response:**
```json
{
  "seatNumber": "12A",
  "status": "CONFIRMED",
  "confirmedAt": "2026-03-15T10:06:30Z"
}
```

---

#### 5. Baggage Validation

```http
POST /api/v1/baggage/validate
```
Validate baggage weight and calculate fees.

**Request Body:**
```json
{
  "checkInId": "550e8400-e29b-41d4-a716-446655440000",
  "weight": 30.0,
  "pieces": 2
}
```

**Response:**
```json
{
  "baggageId": "550e8400-e29b-41d4-a716-446655440003",
  "weight": 30.0,
  "excessWeight": 5.0,
  "excessFee": 50.00,
  "paymentRequired": true,
  "message": "Excess baggage fee required: $50.00"
}
```

---

#### 6. Payment Processing (MOCK)

```http
POST /api/v1/payment/process
```
Process excess baggage payment (mock - always succeeds).

**Request Body:**
```json
{
  "checkInId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 50.00
}
```

**Response:**
```json
{
  "transactionId": "TXN-550e8400-e29b-41d4-a716-446655440004",
  "status": "SUCCESS",
  "amount": 50.00,
  "processedAt": "2026-03-15T10:07:00Z"
}
```

---

## 🔄 Background Workers

### Seat Hold Expiration Job

**Purpose:** Automatically release seats that have been held for 120+ seconds.

**Schedule:** Runs every 10 seconds  
**Mechanism:** Spring Scheduler + ShedLock (distributed locking)  
**Logic:**
1. Find all seat holds where `expires_at < current_time` AND `status = HELD`
2. Update seat status from `HELD` to `AVAILABLE`
3. Update hold status to `EXPIRED`
4. Log expiration details

**Configuration:**
```yaml
app:
  seat-hold:
    expiration-job-rate: 10000  # milliseconds
```

**Distributed Locking:**
Uses ShedLock to ensure only ONE instance of the job runs in a multi-instance deployment.

---

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test Coverage Report

```bash
mvn jacoco:report

# View report at:
# target/site/jacoco/index.html
```

### Load Testing

Use tools like **JMeter**, **Gatling**, or **k6** to test concurrent seat holds:

```bash
# Example with k6 (install first)
k6 run load-tests/seat-hold-test.js
```

---

## 📊 Monitoring & Health Checks

### Health Endpoint

```http
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Metrics Endpoint

```http
GET /actuator/metrics
```

Available metrics:
- `http.server.requests` - Request count and latency
- `jvm.memory.used` - Memory usage
- `hikaricp.connections.active` - Database connections

---

## 🐛 Troubleshooting

### Common Issues

**1. Application won't start - Database connection error**

```
Error: Connection refused - localhost:5432
```

**Solution:** Ensure PostgreSQL is running and accessible.
```bash
docker ps  # Check if postgres container is running
docker logs skyhigh-postgres  # Check logs
```

---

**2. Redis connection error**

```
Error: Unable to connect to Redis at localhost:6379
```

**Solution:** Ensure Redis is running.
```bash
docker ps  # Check if redis container is running
redis-cli ping  # Should return PONG
```

---

**3. Seat hold not expiring**

**Check:** Background job logs
```bash
docker logs skyhigh-app | grep "SeatHoldExpirationJob"
```

**Verify:** ShedLock table exists
```sql
SELECT * FROM shedlock;
```

---

**4. Seat conflict (double booking)**

This should **never** happen. If it does:
1. Check database transaction isolation level
2. Verify optimistic locking is enabled (`version` field)
3. Review logs for concurrency errors

---

## 📚 Documentation

- **[PRD.md](docs/PRD.md)** - Product Requirements Document
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture and design
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Project folder structure
- **[WORKFLOW_DESIGN.md](WORKFLOW_DESIGN.md)** - Implementation workflows and flows
- **[API-SPECIFICATION.yml](API-SPECIFICATION.yml)** - OpenAPI specification
- **[CHAT_HISTORY.md](CHAT_HISTORY.md)** - Design journey and decisions

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 License

This project is proprietary software owned by **SkyHigh Airlines**.

---

## 👥 Team

**SkyHigh Airlines Engineering Team**  
For questions or support, contact: engineering@skyhigh-airlines.com

---

## 🎯 Project Status

**Status:** ✅ Production Ready  
**Version:** 1.0.0-SNAPSHOT  
**Last Updated:** March 15, 2026

---

**Happy Check-In! ✈️**

