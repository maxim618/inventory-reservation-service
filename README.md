# Inventory Reservation Service

High-load backend service that provides atomic inventory reservations
to prevent overselling in marketplaces and e-commerce platforms.

### Use cases
- Flash sales and high-traffic campaigns
- Marketplaces with concurrent purchases
- Multi-service architectures (cart, checkout, order)
- Inventory consistency under high load

## Problem

Overselling is one of the most common and expensive problems in e-commerce systems.

Typical issues:
- Race conditions between cart and checkout
- Inventory inconsistency under high load
- Double selling during flash sales
- Complex rollback logic across services

This service solves these problems by providing
**atomic, time-bound inventory reservations**.

## Solution

Inventory Reservation Service acts as a dedicated infrastructure component
responsible for inventory consistency.

Key principles:
- Atomic reservations
- Time-bound TTL-based locking
- Idempotent API
- Redis-based hot path
- PostgreSQL as source of truth

## Architecture

Client → Reservation API → Redis (atomic ops) → PostgreSQL (persistence)

Redis is used for:
- Atomic stock updates
- Reservation TTL handling
- Idempotency keys

PostgreSQL is used as:
- Source of truth
- Audit log
- Recovery layer

```
[ Client ]
    |
    v
[ Reservation API ]
    |
    +---( Lua Atomic Check )---> [ Redis (Cache/TTL) ]
    |
    +---( Persistent Store )---> [ PostgreSQL ]
```

## Guarantees

- No overselling under concurrent requests
- Idempotent reservation creation
- Automatic expiration of stale reservations
- Predictable latency under load

## API Overview

POST /reservations
POST /reservations/{id}/confirm
POST /reservations/{id}/release
GET  /items/{itemId}/availability


## Non-goals

This service does NOT handle:
- Orders
- Payments
- Catalog management
- Warehouse operations
- UI or frontend logic


## Performance

Designed for high concurrency scenarios:
- Redis Lua scripts for atomic operations
- Optimistic locking for persistence
- Horizontal scalability

Target latency: <10ms for reservation requests

## Tech Stack

- Java 17
- Spring Boot
- Redis / Valkey
- PostgreSQL
- Testcontainers
- Micrometer / Prometheus

## Roadmap

- [ ] Multi-warehouse support
- [ ] Priority reservations
- [ ] Saga-friendly APIs
- [ ] Event streaming integration (Kafka)
- [ ] Read replicas support



