package com.flashsale.service;

import com.flashsale.model.FlashSaleItem;
import com.flashsale.model.Order;
import com.flashsale.model.OrderStatus;
import com.flashsale.model.User;
import com.flashsale.repository.FlashSaleItemRepository;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderService {

    private final UserRepository userRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final OrderRepository orderRepository;

    @Async
    @Transactional
    public void processSuccessfulPurchase(Long userId, Long flashSaleItemId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            FlashSaleItem item = flashSaleItemRepository.findById(flashSaleItemId)
                    .orElseThrow(() -> new IllegalArgumentException("FlashSaleItem not found: " + flashSaleItemId));

            if (user.getBalance().compareTo(item.getFlashPrice()) < 0) {
                throw new IllegalStateException("Insufficient balance for user " + userId);
            }

            user.setBalance(user.getBalance().subtract(item.getFlashPrice()));
            item.setSoldQuantity(item.getSoldQuantity() + 1);
            Order order = Order.builder()
                    .user(user)
                    .flashSaleItem(item)
                    .status(OrderStatus.SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .build();
            orderRepository.save(order);

            log.info("Successfully saved order {} to database", order.getId());
        } catch (Exception e) {
            log.error("Failed to process successful purchase for user {} and item {}: {}", userId, flashSaleItemId, e.getMessage());
            throw e;
        }
    }
}
