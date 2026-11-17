package com.openshelves;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main entry point for the OpenShelves Spring Boot application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenShelvesApplication {

    static void main (String... args) {
        SpringApplication.run(OpenShelvesApplication.class, args);
    }
}
