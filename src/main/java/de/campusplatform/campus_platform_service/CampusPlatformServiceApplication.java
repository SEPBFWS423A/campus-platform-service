package de.campusplatform.campus_platform_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CampusPlatformServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusPlatformServiceApplication.class, args);
	}

}
