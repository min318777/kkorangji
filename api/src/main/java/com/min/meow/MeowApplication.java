package com.min.meow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = {"com.min.meow", "com.min.kafka"})
public class MeowApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeowApplication.class, args);
	}

}
