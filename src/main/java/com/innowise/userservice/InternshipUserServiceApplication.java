package com.innowise.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class InternshipUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternshipUserServiceApplication.class, args);
    }

}
