package com.openshelves;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ServerApplication {

    static void main(String... args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
