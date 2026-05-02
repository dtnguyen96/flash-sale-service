package com.flashsale.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDate;

import static com.flashsale.service.FlashSaleRedisService.PurchaseResult.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private FlashSaleRedisService service;

    private static final long ITEM_ID = 1L;
    private static final long USER_ID = 42L;
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);

    @Test
    void tryPurchase_scriptReturnsOne_returnsSuccess() {
        givenScriptReturns(1L);

        assertThat(service.tryPurchase(ITEM_ID, USER_ID, TODAY)).isEqualTo(SUCCESS);
    }

    @Test
    void tryPurchase_scriptReturnsNegativeOne_returnsAlreadyPurchased() {
        givenScriptReturns(-1L);

        assertThat(service.tryPurchase(ITEM_ID, USER_ID, TODAY)).isEqualTo(ALREADY_PURCHASED);
    }

    @Test
    void tryPurchase_scriptReturnsNegativeTwo_returnsOutOfStock() {
        givenScriptReturns(-2L);

        assertThat(service.tryPurchase(ITEM_ID, USER_ID, TODAY)).isEqualTo(OUT_OF_STOCK);
    }

    @Test
    void tryPurchase_scriptReturnsNull_returnsOutOfStock() {
        givenScriptReturns(null);

        assertThat(service.tryPurchase(ITEM_ID, USER_ID, TODAY)).isEqualTo(OUT_OF_STOCK);
    }

    @Test
    void tryPurchase_passesCorrectKeysAndMemberToScript() {
        givenScriptReturns(1L);

        service.tryPurchase(ITEM_ID, USER_ID, TODAY);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of(
                        "flash_sale:user_purchases:2026-05-01",
                        "flash_sale:item:1:stock"
                )),
                org.mockito.ArgumentMatchers.eq("42:1")
        );
    }

    @SuppressWarnings("unchecked")
    private void givenScriptReturns(Long value) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(value);
    }
}
