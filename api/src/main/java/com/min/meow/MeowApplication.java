package com.min.meow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.min.meow", "com.min.kafka"})
public class MeowApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeowApplication.class, args);
	}

}
