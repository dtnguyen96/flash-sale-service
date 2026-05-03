package com.flashsale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FlashSaleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlashSaleServiceApplication.class, args);
	}

}
