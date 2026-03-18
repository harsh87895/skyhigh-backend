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
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                     External Systems                            â”‚
â”‚                                                                 â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                     â”‚
â”‚  â”‚   Passenger  â”‚         â”‚   Airport    â”‚                     â”‚
â”‚  â”‚   (Mobile)   â”‚         â”‚    Kiosk     â”‚                     â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”˜         â””â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”˜                     â”‚
â”‚          â”‚                        â”‚                            â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â”‚                        â”‚
           â”‚      HTTPS / REST      â”‚
           â”‚                        â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                  Load Balancer (Future)                        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                            â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚               SkyHigh Core Application Layer                   â”‚
â”‚                                                                 â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚  â”‚          Spring Boot REST API (Stateless)              â”‚   â”‚
â”‚  â”‚                                                         â”‚   â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”             â”‚   â”‚
â”‚  â”‚  â”‚ Check-In â”‚  â”‚   Seat   â”‚  â”‚ Baggage  â”‚             â”‚   â”‚
â”‚  â”‚  â”‚Controllerâ”‚  â”‚Controllerâ”‚  â”‚Controllerâ”‚             â”‚   â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜             â”‚   â”‚
â”‚  â”‚       â”‚             â”‚             â”‚                    â”‚   â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”             â”‚   â”‚
â”‚  â”‚  â”‚        Business Logic Layer          â”‚             â”‚   â”‚
â”‚  â”‚  â”‚  (Services with Transaction Mgmt)    â”‚             â”‚   â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜             â”‚   â”‚
â”‚  â”‚       â”‚             â”‚             â”‚                    â”‚   â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”             â”‚   â”‚
â”‚  â”‚  â”‚     Data Access Layer (Repositories) â”‚             â”‚   â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜             â”‚   â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â”‚          â”‚             â”‚             â”‚                        â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”       â”‚       â”Œâ”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”            â”‚
â”‚  â”‚ Background  â”‚       â”‚       â”‚   Cache Layer  â”‚            â”‚
â”‚  â”‚    Jobs     â”‚       â”‚       â”‚   (Redis)      â”‚            â”‚
â”‚  â”‚ (Scheduler) â”‚       â”‚       â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜            â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜       â”‚                                      â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                         â”‚
                         â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                  Persistence Layer                            â”‚
â”‚                                                                â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”          â”‚
â”‚  â”‚   PostgreSQL     â”‚          â”‚      Redis       â”‚          â”‚
â”‚  â”‚   (Primary DB)   â”‚          â”‚     (Cache)      â”‚          â”‚
â”‚  â”‚                  â”‚          â”‚                  â”‚          â”‚
â”‚  â”‚  - ACID trans    â”‚          â”‚  - TTL: 2s       â”‚          â”‚
â”‚  â”‚  - Optimistic    â”‚          â”‚  - Seat maps     â”‚          â”‚
â”‚  â”‚    locking       â”‚          â”‚                  â”‚          â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜          â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## 3. Component Architecture

### Layered Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    Presentation Layer                       â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”â”‚
â”‚  â”‚ CheckIn   â”‚  â”‚  Flight   â”‚  â”‚   Seat    â”‚  â”‚ Baggage  â”‚â”‚
â”‚  â”‚Controller â”‚  â”‚ Controllerâ”‚  â”‚ Controllerâ”‚  â”‚Controllerâ”‚â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜â”‚
â”‚        â”‚              â”‚              â”‚              â”‚       â”‚
â”‚        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜       â”‚
â”‚                         â”‚                                   â”‚
â”‚              REST API (JSON over HTTP)                      â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    Service Layer (Business Logic)           â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”     â”‚
â”‚  â”‚  CheckIn     â”‚  â”‚    Seat      â”‚  â”‚   Baggage    â”‚     â”‚
â”‚  â”‚  Service     â”‚  â”‚  Service     â”‚  â”‚   Service    â”‚     â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜     â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”     â”‚
â”‚  â”‚  SeatHold    â”‚  â”‚   Payment    â”‚  â”‚   Flight     â”‚     â”‚
â”‚  â”‚  Service     â”‚  â”‚  Service     â”‚  â”‚   Service    â”‚     â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜     â”‚
â”‚                                                             â”‚
â”‚  Responsibilities:                                          â”‚
â”‚  - Business rule validation                                 â”‚
â”‚  - Transaction management (@Transactional)                  â”‚
â”‚  - Orchestration between entities                           â”‚
â”‚  - Exception handling                                       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚              Data Access Layer (Repositories)               â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”‚
â”‚  â”‚ Passenger  â”‚ â”‚   Flight   â”‚ â”‚    Seat    â”‚ â”‚CheckIn  â”‚ â”‚
â”‚  â”‚ Repository â”‚ â”‚ Repository â”‚ â”‚ Repository â”‚ â”‚Repositryâ”‚ â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                             â”‚
â”‚  â”‚  SeatHold  â”‚ â”‚  Baggage   â”‚                             â”‚
â”‚  â”‚ Repository â”‚ â”‚ Repository â”‚                             â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                             â”‚
â”‚                                                             â”‚
â”‚  Responsibilities:                                          â”‚
â”‚  - CRUD operations (Spring Data JPA)                        â”‚
â”‚  - Custom queries                                           â”‚
â”‚  - Optimistic locking                                       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    Domain Model Layer                       â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”‚
â”‚  â”‚ Passenger  â”‚ â”‚   Flight   â”‚ â”‚    Seat    â”‚ â”‚CheckIn  â”‚ â”‚
â”‚  â”‚  Entity    â”‚ â”‚   Entity   â”‚ â”‚   Entity   â”‚ â”‚ Entity  â”‚ â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                             â”‚
â”‚  â”‚  SeatHold  â”‚ â”‚  Baggage   â”‚                             â”‚
â”‚  â”‚   Entity   â”‚ â”‚   Entity   â”‚                             â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                             â”‚
â”‚                                                             â”‚
â”‚  + Enums: SeatStatus, CheckInStatus, SeatClass, etc.       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

### Cross-Cutting Concerns

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                  Cross-Cutting Concerns                     â”‚
â”‚                                                             â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”        â”‚
â”‚  â”‚   Caching   â”‚  â”‚   Logging   â”‚  â”‚  Exception  â”‚        â”‚
â”‚  â”‚  (Redis)    â”‚  â”‚  (Slf4j)    â”‚  â”‚   Handling  â”‚        â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜        â”‚
â”‚                                                             â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”        â”‚
â”‚  â”‚ Scheduling  â”‚  â”‚ Validation  â”‚  â”‚  Monitoring â”‚        â”‚
â”‚  â”‚ (ShedLock)  â”‚  â”‚  (Jakarta)  â”‚  â”‚  (Actuator) â”‚        â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜        â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## 4. Database Schema

### Entity Relationship Diagram (ERD)

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚   passengers     â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚ passenger_id (PK)â”‚â”€â”€â”€â”
â”‚ first_name       â”‚   â”‚
â”‚ last_name        â”‚   â”‚
â”‚ email            â”‚   â”‚
â”‚ phone            â”‚   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
                       â”‚
                       â”‚ 1
                       â”‚
                       â”‚ *
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚     flights      â”‚   â”‚      â”‚   check_ins      â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤   â”‚      â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚ flight_id (PK)   â”‚â”€â”€â”€â”¼â”€â”€â”€â”€â”€â–¶â”‚ check_in_id (PK) â”‚
â”‚ flight_number    â”‚   â”‚   â”Œâ”€â”€â”‚ passenger_id (FK)â”‚
â”‚ origin           â”‚   â”‚   â”‚  â”‚ flight_id (FK)   â”‚
â”‚ destination      â”‚   â”‚   â”‚  â”‚ booking_referenceâ”‚
â”‚ departure_time   â”‚   â””â”€â”€â”€â”˜  â”‚ seat_number      â”‚
â”‚ arrival_time     â”‚          â”‚ status           â”‚
â”‚ status           â”‚          â”‚ baggage_weight   â”‚
â”‚ check_in_opens_atâ”‚          â”‚ payment_required â”‚
â”‚check_in_closes_atâ”‚          â”‚ payment_status   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜          â”‚ created_at       â”‚
        â”‚                     â”‚ completed_at     â”‚
        â”‚ 1                   â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
        â”‚                              â”‚
        â”‚ *                            â”‚ 1
        â”‚                              â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                   â”‚ *
â”‚      seats       â”‚                   â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ seat_id (PK)     â”‚â”€â”€â”€â”      â”‚    baggage       â”‚
â”‚ flight_id (FK)   â”‚   â”‚      â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚ seat_number      â”‚   â”‚      â”‚ baggage_id (PK)  â”‚
â”‚ row_number       â”‚   â”‚      â”‚ check_in_id (FK) â”‚
â”‚ column_letter    â”‚   â”‚      â”‚ weight           â”‚
â”‚ seat_class       â”‚   â”‚      â”‚ pieces           â”‚
â”‚ position         â”‚   â”‚      â”‚ excess_weight    â”‚
â”‚ status           â”‚   â”‚      â”‚ excess_fee       â”‚
â”‚ price            â”‚   â”‚      â”‚ payment_txn_id   â”‚
â”‚ version âš¡       â”‚   â”‚      â”‚ validated_at     â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚      â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
                       â”‚ 1
                       â”‚
                       â”‚ *
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚   seat_holds     â”‚
              â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
              â”‚ hold_id (PK)     â”‚
              â”‚ seat_id (FK)     â”‚
              â”‚ check_in_id (FK) â”‚
              â”‚ status           â”‚
              â”‚ held_at          â”‚
              â”‚ expires_at       â”‚
              â”‚ confirmed_at     â”‚
              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜

âš¡ = Optimistic locking version field
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
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Client   â”‚
â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”˜
      â”‚ 1. POST /api/v1/seats/hold
      â”‚    {seatId, checkInId}
      â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ SeatController  â”‚
â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
      â”‚ 2. Validate request
      â”‚
      â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ SeatHoldService  â”‚
â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
      â”‚ 3. @Transactional begin
      â”‚
      â”œâ”€â”€â–¶ SeatRepository.findBySeatIdAndStatus(seatId, AVAILABLE)
      â”‚    â”œâ”€ SELECT * FROM seats
      â”‚    â”‚  WHERE seat_id = ? AND status = 'AVAILABLE' AND version = ?
      â”‚    â”‚  FOR UPDATE (optimistic lock)
      â”‚    â”‚
      â”‚    â””â”€ If not found: throw SeatUnavailableException
      â”‚
      â”œâ”€â”€â–¶ seat.setStatus(HELD)
      â”‚    seat.setVersion(version + 1)
      â”‚
      â”œâ”€â”€â–¶ SeatRepository.save(seat)
      â”‚    â”œâ”€ UPDATE seats
      â”‚    â”‚  SET status = 'HELD', version = version + 1
      â”‚    â”‚  WHERE seat_id = ? AND version = ?
      â”‚    â”‚
      â”‚    â””â”€ If rows affected = 0: throw OptimisticLockException
      â”‚
      â”œâ”€â”€â–¶ Create SeatHold entity
      â”‚    â”œâ”€ holdId = UUID.randomUUID()
      â”‚    â”œâ”€ seatId = seat.getSeatId()
      â”‚    â”œâ”€ checkInId = request.getCheckInId()
      â”‚    â”œâ”€ status = HELD
      â”‚    â”œâ”€ heldAt = Instant.now()
      â”‚    â””â”€ expiresAt = heldAt.plusSeconds(120)
      â”‚
      â”œâ”€â”€â–¶ SeatHoldRepository.save(hold)
      â”‚    â””â”€ INSERT INTO seat_holds (...)
      â”‚
      â””â”€â”€â–¶ @Transactional commit
           â”‚
           â–¼
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
â”€â”€â”€â”€â”€   â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€   â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€   â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
T0      Read seat 12A (version=5)      seat 12A: version=5        Read seat 12A (version=5)
        
T1      Modify: status = HELD          
        
T2      UPDATE seats                   UPDATE executed
        SET status='HELD',             version incremented to 6
        version=6                      Passenger A: SUCCESS âœ“
        WHERE id=? AND version=5
        
T3                                                                 UPDATE seats
                                                                   SET status='HELD',
                                                                   version=6
                                                                   WHERE id=? AND version=5
                                                                   
T4                                      No rows updated!           Passenger B: FAIL âœ—
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
    
    @Version  // â† Enables optimistic locking
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
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  Client  â”‚
â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”˜
      â”‚ GET /api/v1/flights/{flightId}/seats
      â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚SeatService  â”‚
â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜
      â”‚
      â”œâ”€ Check Redis cache: key="seatMap:{flightId}"
      â”‚
      â–¼
  â”Œâ”€â”€â”€â”€â”€â”€â”€â”
  â”‚ Cache â”‚
  â”‚ Hit?  â”‚
  â””â”€â”€â”€â”¬â”€â”€â”€â”˜
      â”‚
      â”œâ”€ YES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
      â”‚                                  â”‚
      â”‚                                  â–¼
      â”‚                          Return cached data
      â”‚                          (Fast path)
      â”‚
      â””â”€ NO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                                         â”‚
                                         â–¼
                                 Query PostgreSQL
                                 SELECT * FROM seats
                                 WHERE flight_id = ?
                                         â”‚
                                         â–¼
                                 Store in Redis
                                 TTL = 2 seconds
                                         â”‚
                                         â–¼
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
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚               Application Instance 1                        â”‚
â”‚                                                             â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚  â”‚  Spring Scheduler (Cron: every 10s)                 â”‚   â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â”‚              â”‚                                             â”‚
â”‚              â–¼                                             â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚  â”‚  ShedLock - Acquire Lock "SeatHoldExpirationJob"   â”‚   â”‚
â”‚  â”‚  - lockAtLeastFor: 5s                               â”‚   â”‚
â”‚  â”‚  - lockAtMostFor: 9s                                â”‚   â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â”‚              â”‚                                             â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
               â”‚
               â–¼
      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
      â”‚  PostgreSQL    â”‚
      â”‚  shedlock      â”‚
      â”‚  table         â”‚
      â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜
               â”‚ Lock acquired (or skip if locked)
               â”‚
               â–¼
    Execute job only on ONE instance
    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
    â”‚ 1. Find expired holds:          â”‚
    â”‚    SELECT * FROM seat_holds     â”‚
    â”‚    WHERE status='HELD'          â”‚
    â”‚      AND expires_at < NOW()     â”‚
    â”‚                                 â”‚
    â”‚ 2. For each expired hold:       â”‚
    â”‚    - Update seat: HELDâ†’AVAILABLEâ”‚
    â”‚    - Update hold: HELDâ†’EXPIRED  â”‚
    â”‚                                 â”‚
    â”‚ 3. Log expiration               â”‚
    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
               â”‚
               â–¼
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
                     â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                     â”‚Load Balancer â”‚
                     â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜
                            â”‚
          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
          â”‚                 â”‚                 â”‚
          â–¼                 â–¼                 â–¼
    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
    â”‚ App      â”‚      â”‚ App      â”‚      â”‚ App      â”‚
    â”‚Instance 1â”‚      â”‚Instance 2â”‚      â”‚Instance 3â”‚
    â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜      â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜      â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜
         â”‚                 â”‚                 â”‚
         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                           â”‚
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚                         â”‚
              â–¼                         â–¼
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â”‚PostgreSQLâ”‚              â”‚  Redis   â”‚
        â”‚ (Shared) â”‚              â”‚ (Shared) â”‚
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
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
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                       Cloud Provider                       â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”‚
â”‚  â”‚         Application Load Balancer (ALB)              â”‚ â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚
â”‚                       â”‚                                    â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”‚
â”‚  â”‚  Auto Scaling Group â”‚                                 â”‚ â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”                 â”‚ â”‚
â”‚  â”‚  â”‚App (K8sâ”‚  â”‚App (K8sâ”‚  â”‚App (K8sâ”‚                 â”‚ â”‚
â”‚  â”‚  â”‚  Pod)  â”‚  â”‚  Pod)  â”‚  â”‚  Pod)  â”‚                 â”‚ â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”˜                 â”‚ â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚
â”‚                       â”‚                                    â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”â”‚ â”‚
â”‚  â”‚  â”‚   RDS          â”‚â”‚  â”‚   ElastiCache (Redis)      â”‚â”‚ â”‚
â”‚  â”‚  â”‚  (PostgreSQL)  â”‚â”‚  â”‚   - Multi-AZ               â”‚â”‚ â”‚
â”‚  â”‚  â”‚  - Multi-AZ    â”‚â”‚  â”‚   - Cluster mode           â”‚â”‚ â”‚
â”‚  â”‚  â”‚  - Read replicaâ”‚â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜â”‚ â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜â”‚                                 â”‚ â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## Summary

### Architectural Highlights

1. **Layered Architecture:** Clear separation of concerns (Controller â†’ Service â†’ Repository â†’ Entity)
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

