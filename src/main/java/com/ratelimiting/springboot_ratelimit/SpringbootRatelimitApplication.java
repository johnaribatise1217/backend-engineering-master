package com.ratelimiting.springboot_ratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringbootRatelimitApplication {

	static{
		com.ratelimiting.springboot_ratelimit.EnvLoader envLoader = 
		new com.ratelimiting.springboot_ratelimit.EnvLoader();
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringbootRatelimitApplication.class, args);
	}

}
