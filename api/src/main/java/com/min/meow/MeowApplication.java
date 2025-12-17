package com.min.meow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableCaching
@SpringBootApplication(scanBasePackages = "com.min.meow")
@EnableJpaRepositories(basePackages = {
		"com.min.meow.post.repository",
		"com.min.meow.post.comment.repository",
		"com.min.meow.post.postlike.repository",
		"com.min.meow.user.repository",
		"com.min.meow.notification.repository"  // notification repository 스캔 추가
})
@EntityScan(basePackages = {
		"com.min.meow.global",
		"com.min.meow.post.entity",
		"com.min.meow.post.comment.entity",
		"com.min.meow.post.postlike.entity",
		"com.min.meow.user.entity",
		"com.min.meow.notification.entity"  // notification entity 스캔 추가
})

public class MeowApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeowApplication.class, args);
	}

}
