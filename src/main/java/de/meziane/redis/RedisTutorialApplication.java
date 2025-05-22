package de.meziane.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class RedisTutorialApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisTutorialApplication.class, args);
	}

}
