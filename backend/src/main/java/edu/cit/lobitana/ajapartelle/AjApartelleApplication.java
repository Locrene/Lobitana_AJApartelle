package edu.cit.lobitana.ajapartelle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AjApartelleApplication {
    public static void main(String[] args) {
        SpringApplication.run(AjApartelleApplication.class, args);
    }
}
