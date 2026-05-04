package com.flashsale.service;

import com.flashsale.model.FlashSaleItem;
import com.flashsale.model.Order;
import com.flashsale.model.OrderStatus;
import com.flashsale.model.User;
import com.flashsale.repository.FlashSaleItemRepository;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlashSaleOrderServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FlashSaleItemRepository flashSaleItemRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private FlashSaleOrderService service;

    private static final long USER_ID  = 1L;
    private static final long ITEM_ID  = 10L;

    @Test
    void processSuccessfulPurchase_sufficientBalance_savesOrderAndDeductsBalance() {
        User user         = user(new BigDecimal("50.00"));
        FlashSaleItem item = item(new BigDecimal("19.99"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(flashSaleItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processSuccessfulPurchase(USER_ID, ITEM_ID);

        assertThat(user.getBalance()).isEqualByComparingTo("30.01");
        assertThat(item.getSoldQuantity()).isEqualTo(1);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getFlashSaleItem()).isEqualTo(item);
    }

    @Test
    void processSuccessfulPurchase_exactBalance_succeeds() {
        User user         = user(new BigDecimal("19.99"));
        FlashSaleItem item = item(new BigDecimal("19.99"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(flashSaleItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        service.processSuccessfulPurchase(USER_ID, ITEM_ID);

        assertThat(user.getBalance()).isEqualByComparingTo("0.00");
        verify(orderRepository).save(any());
    }

    @Test
    void processSuccessfulPurchase_insufficientBalance_throwsAndLeavesStateUnchanged() {
        User user         = user(new BigDecimal("5.00"));
        FlashSaleItem item = item(new BigDecimal("19.99"));

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(flashSaleItemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.processSuccessfulPurchase(USER_ID, ITEM_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.valueOf(USER_ID));

        assertThat(user.getBalance()).isEqualByComparingTo("5.00");
        assertThat(item.getSoldQuantity()).isZero();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void processSuccessfulPurchase_userNotFound_throwsIllegalArgumentException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processSuccessfulPurchase(USER_ID, ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(USER_ID));

        verifyNoInteractions(flashSaleItemRepository, orderRepository);
    }

    @Test
    void processSuccessfulPurchase_itemNotFound_throwsIllegalArgumentException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(BigDecimal.TEN)));
        when(flashSaleItemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processSuccessfulPurchase(USER_ID, ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(ITEM_ID));

        verifyNoInteractions(orderRepository);
    }

    private User user(BigDecimal balance) {
        return User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .balance(balance)
                .build();
    }

    private FlashSaleItem item(BigDecimal flashPrice) {
        return FlashSaleItem.builder()
                .flashPrice(flashPrice)
                .soldQuantity(0)
                .allocatedQuantity(10)
                .build();
    }
}
