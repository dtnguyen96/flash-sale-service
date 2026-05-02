package com.flashsale.repository;

import com.flashsale.model.FlashSale;
import com.flashsale.model.FlashSaleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class FlashSaleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FlashSaleRepository flashSaleRepository;

    @Test
    void findByStatus_active_returnsOnlyActiveSales() {
        em.persistAndFlush(sale("Summer Sale", FlashSaleStatus.ACTIVE));
        em.persistAndFlush(sale("Winter Sale", FlashSaleStatus.UPCOMING));

        List<FlashSale> result = flashSaleRepository.findByStatus(FlashSaleStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Summer Sale");
    }

    @Test
    void findByStatus_noMatchingStatus_returnsEmpty() {
        em.persistAndFlush(sale("Ended Sale", FlashSaleStatus.ENDED));

        assertThat(flashSaleRepository.findByStatus(FlashSaleStatus.ACTIVE)).isEmpty();
    }

    @Test
    void findByStatus_multipleMatchingStatus_returnsAll() {
        em.persistAndFlush(sale("Sale A", FlashSaleStatus.UPCOMING));
        em.persistAndFlush(sale("Sale B", FlashSaleStatus.UPCOMING));
        em.persistAndFlush(sale("Sale C", FlashSaleStatus.ACTIVE));

        List<FlashSale> upcoming = flashSaleRepository.findByStatus(FlashSaleStatus.UPCOMING);

        assertThat(upcoming).hasSize(2)
                .extracting(FlashSale::getName)
                .containsExactlyInAnyOrder("Sale A", "Sale B");
    }

    private FlashSale sale(String name, FlashSaleStatus status) {
        return FlashSale.builder()
                .name(name)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .status(status)
                .build();
    }
}
