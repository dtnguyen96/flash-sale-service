package com.flashsale.repository;

import com.flashsale.model.Order;
import com.flashsale.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByFlashSaleItemId(Long flashSaleItemId);

    boolean existsByUserIdAndFlashSaleItemId(Long userId, Long flashSaleItemId);

    List<Order> findByStatus(OrderStatus status);
}
