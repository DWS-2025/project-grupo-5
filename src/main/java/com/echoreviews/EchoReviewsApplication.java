package com.echoreviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EchoReviewsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EchoReviewsApplication.class, args);
	}

}
