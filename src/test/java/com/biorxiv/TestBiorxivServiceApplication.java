package com.biorxiv;

import org.springframework.boot.SpringApplication;

public class TestBiorxivServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(BiorxivServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
