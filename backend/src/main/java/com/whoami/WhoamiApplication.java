package com.whoami;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.whoami")
public class WhoamiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhoamiApplication.class, args);
    }
}
