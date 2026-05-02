# Flash Sale Service - System Architecture

## Constraints
- **Goal:** Handle 500+ TPS and be multi-instance ready (stateless).
- **Core Rule 1:** Do not oversell. Limit strictly enforced.
- **Core Rule 2:** 1 quantity limit per specific item per day. (Assumption: Users are allowed to buy different items from different flash sales on the same day, but cannot buy the same item twice).
- **Stack:** Java 21 (Virtual Threads enabled), Spring Boot 4, PostgreSQL, Redis.

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
    - `flash_sale:user_purchases:{date}` (Value: Set of user_id:item_id) -> Note: Storing user_id combined with item_id enforces the specific-item limit.
- Flow: Redis deducts stock instantly -> returns 200 OK -> Async worker saves to PostgreSQL.