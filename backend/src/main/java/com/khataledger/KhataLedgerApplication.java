package com.khataledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Khata Ledger Service.
 *
 * @SpringBootApplication is a meta-annotation that combines:
 *   - @Configuration   : marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration : tells Spring Boot to auto-configure based on classpath
 *   - @ComponentScan   : scans the current package and sub-packages for @Component/@Service/@Repository/@RestController
 *
 * @EnableScheduling lets us use @Scheduled on the overdue-reminder job.
 * @EnableAsync lets us fire reminders without blocking the scheduler thread.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class KhataLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KhataLedgerApplication.class, args);
    }
}
