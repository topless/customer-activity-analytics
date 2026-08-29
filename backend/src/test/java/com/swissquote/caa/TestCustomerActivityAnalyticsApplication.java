package com.swissquote.caa;

import org.springframework.boot.SpringApplication;

public class TestCustomerActivityAnalyticsApplication {

	public static void main(String[] args) {
		SpringApplication.from(CustomerActivityAnalyticsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
