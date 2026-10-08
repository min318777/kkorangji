package com.min.meow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;


@EnableRetry
@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
public class MeowApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeowApplication.class, args);
	}

}