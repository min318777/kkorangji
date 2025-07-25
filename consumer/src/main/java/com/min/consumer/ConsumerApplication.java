package com.min.consumer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.min.consumer",
        "com.min.meow.notification.entity",
        "com.min.meow.notification.repository",
        "com.min.meow.notification.service",
        "com.min.kafka.dto"
})
@EnableJpaRepositories(basePackages = "com.min.meow.notification.repository")
@EntityScan(basePackages = "com.min.meow.notification.entity")
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

}
