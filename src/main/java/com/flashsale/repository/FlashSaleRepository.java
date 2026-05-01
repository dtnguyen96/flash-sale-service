package com.flashsale.repository;

import com.flashsale.model.FlashSale;
import com.flashsale.model.FlashSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    List<FlashSale> findByStatus(FlashSaleStatus status);
}
