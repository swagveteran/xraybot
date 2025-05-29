package com.xraybot;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XrayBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(XrayBotApplication.class, args);
	}
}
