package com.flashsale.controller;

import com.flashsale.service.FlashSaleOrderService;
import com.flashsale.service.FlashSaleRedisService;
import com.flashsale.service.FlashSaleRedisService.PurchaseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleRedisService flashSaleRedisService;
    private final FlashSaleOrderService flashSaleOrderService;

    public record BuyRequest(Long flashSaleItemId) {}

    @PostMapping("/buy")
    public ResponseEntity<String> buy(@RequestBody BuyRequest request, Authentication authentication) {
        long userId = Long.parseLong(authentication.getName());

        PurchaseResult result = flashSaleRedisService.tryPurchase(
                request.flashSaleItemId(), userId, LocalDate.now());

        return switch (result) {
            case SUCCESS -> {
                flashSaleOrderService.processSuccessfulPurchase(userId, request.flashSaleItemId());
                yield ResponseEntity.ok("Purchase successful.");
            }
            case ALREADY_PURCHASED -> ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("You have already purchased this item today.");
            case OUT_OF_STOCK -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("This item is out of stock.");
        };
    }
}
