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
		// Spring Boot 애플리케이션 부트스트랩 (내장 톰캣 기동 + ApplicationContext 초기화)
		SpringApplication.run(MeowApplication.class, args);
	}

}