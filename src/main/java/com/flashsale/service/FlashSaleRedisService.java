package com.flashsale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashSaleRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // KEYS[1] = flash_sale:user_purchases:{date}  (Set)
    // KEYS[2] = flash_sale:item:{id}:stock        (int)
    // ARGV[1] = {userId}:{itemId}                 (Set member)
    //
    // Returns:  1 = success
    //          -1 = user already purchased this item today
    //          -2 = out of stock
    private static final DefaultRedisScript<Long> PURCHASE_SCRIPT = new DefaultRedisScript<>("""
            local purchase_key = KEYS[1]
            local stock_key    = KEYS[2]
            local member       = ARGV[1]

            if redis.call('SISMEMBER', purchase_key, member) == 1 then
                return -1
            end

            local stock = tonumber(redis.call('GET', stock_key))
            if stock == nil or stock <= 0 then
                return -2
            end

            redis.call('DECR', stock_key)
            redis.call('SADD', purchase_key, member)
            -- Keep the purchase set for 2 days to cover timezone edge cases
            redis.call('EXPIRE', purchase_key, 172800)

            return 1
            """, Long.class);

    public enum PurchaseResult {
        SUCCESS, ALREADY_PURCHASED, OUT_OF_STOCK
    }

    public PurchaseResult tryPurchase(long itemId, long userId, LocalDate date) {
        String purchaseKey = "flash_sale:user_purchases:" + date;
        String stockKey    = "flash_sale:item:" + itemId + ":stock";
        String member      = userId + ":" + itemId;

        Long result = redisTemplate.execute(PURCHASE_SCRIPT, List.of(purchaseKey, stockKey), member);

        if (result == null) return PurchaseResult.OUT_OF_STOCK;
        if (result == 1L)   return PurchaseResult.SUCCESS;
        if (result == -1L)  return PurchaseResult.ALREADY_PURCHASED;
        return PurchaseResult.OUT_OF_STOCK;
    }
}
