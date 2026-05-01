# Flash Sale Service - System Architecture

## Constraints
- **Goal:** Handle 500+ TPS and be multi-instance ready (stateless).
- **Core Rule 1:** Do not oversell. Limit strictly enforced.
- **Core Rule 2:** 1 item per user per day.
- **Stack:** Java 21 (Virtual Threads enabled), Spring Boot 3, PostgreSQL, Redis.

## Database Schema (PostgreSQL)
- `users`: id, email, phone, password_hash, balance
- `products`: id, name, inventory_count
- `flash_sales`: id, name, start_time, end_time, status
- `flash_sale_items`: id, flash_sale_id, product_id, flash_price, allocated_quantity, sold_quantity
- `orders`: id, user_id, flash_sale_item_id, status, created_at

## Concurrency Strategy (Redis)
- Do NOT use PostgreSQL for stock deduction during the sale.
- Use Redis Lua scripts for atomic operations.
- Redis Keys:
    - `flash_sale:item:{id}:stock` (Value: int)
    - `flash_sale:user_purchases:{date}` (Value: Set of user_id:item_id)
- Flow: Redis deducts stock instantly -> returns 200 OK -> Async worker saves to PostgreSQL.