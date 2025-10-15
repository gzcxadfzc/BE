package com.pkg.jpa;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.pkg.jpa")
@EnableJpaRepositories(basePackages = "com.pkg.jpa")
@ComponentScan(basePackages = "com.pkg.jpa")
@ActiveProfiles("test")
public class JpaTestConfig {
}
