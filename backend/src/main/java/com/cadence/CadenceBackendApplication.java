package com.cadence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cadence Core Backend Application.
 * System of record, FSRS-4.5 engine, duration bias calibrator, and burnout guard.
 */
@SpringBootApplication
public class CadenceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CadenceBackendApplication.class, args);
    }
}
