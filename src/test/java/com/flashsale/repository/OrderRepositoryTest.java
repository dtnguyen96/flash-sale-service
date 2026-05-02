package com.flashsale.repository;

import com.flashsale.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private OrderRepository orderRepository;

    private User user;
    private FlashSaleItem item;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .balance(BigDecimal.TEN)
                .build());

        Product product = em.persistAndFlush(Product.builder()
                .name("Widget")
                .inventoryCount(100)
                .build());

        FlashSale sale = em.persistAndFlush(FlashSale.builder()
                .name("Flash Sale")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status(FlashSaleStatus.ACTIVE)
                .build());

        item = em.persistAndFlush(FlashSaleItem.builder()
                .flashSale(sale)
                .product(product)
                .flashPrice(new BigDecimal("9.99"))
                .allocatedQuantity(50)
                .soldQuantity(0)
                .build());
    }

    @Test
    void existsByUserIdAndFlashSaleItemId_orderExists_returnsTrue() {
        persistOrder(user, item, OrderStatus.CONFIRMED);

        assertThat(orderRepository.existsByUserIdAndFlashSaleItemId(user.getId(), item.getId())).isTrue();
    }

    @Test
    void existsByUserIdAndFlashSaleItemId_noOrder_returnsFalse() {
        // enforces the "1 item per user" rule can be checked before inserting
        assertThat(orderRepository.existsByUserIdAndFlashSaleItemId(user.getId(), item.getId())).isFalse();
    }

    @Test
    void findByUserId_returnsOnlyThatUsersOrders() {
        User other = em.persistAndFlush(User.builder()
                .email("other@example.com")
                .passwordHash("hash")
                .balance(BigDecimal.TEN)
                .build());
        persistOrder(user, item, OrderStatus.CONFIRMED);
        persistOrder(other, item, OrderStatus.PENDING);

        List<Order> orders = orderRepository.findByUserId(user.getId());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByStatus_returnsOnlyMatchingStatus() {
        persistOrder(user, item, OrderStatus.CONFIRMED);

        assertThat(orderRepository.findByStatus(OrderStatus.CONFIRMED)).hasSize(1);
        assertThat(orderRepository.findByStatus(OrderStatus.PENDING)).isEmpty();
    }

    @Test
    void findByFlashSaleItemId_returnsAllOrdersForItem() {
        User second = em.persistAndFlush(User.builder()
                .email("second@example.com")
                .passwordHash("hash")
                .balance(BigDecimal.TEN)
                .build());
        persistOrder(user, item, OrderStatus.CONFIRMED);
        persistOrder(second, item, OrderStatus.CONFIRMED);

        assertThat(orderRepository.findByFlashSaleItemId(item.getId())).hasSize(2);
    }

    private void persistOrder(User u, FlashSaleItem i, OrderStatus status) {
        em.persistAndFlush(Order.builder()
                .user(u)
                .flashSaleItem(i)
                .status(status)
                .build());
    }
}
