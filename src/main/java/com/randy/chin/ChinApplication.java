package com.randy.chin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.randy.chin.mapper")
public class ChinApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChinApplication.class, args);
		System.out.println("DEFAULT_TEMPLATE_PATH: " + System.getenv("DEFAULT_TEMPLATE_PATH"));
		System.out.println("DEFAULT_TEMPLATE_PATH: " + System.getenv("DEFAULT_TEMPLATE_PATH"));
	}

}
