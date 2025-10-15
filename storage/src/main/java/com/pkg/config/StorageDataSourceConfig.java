package com.pkg.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class StorageDataSourceConfig {

        @Bean
        @ConfigurationProperties(prefix = "storage.datasource")
        public HikariConfig storageHikariConfig() {
            return new HikariConfig();
        }

        @Bean(name = "storageDataSource")
        public DataSource storageDataSource(HikariConfig storageHikariConfig) {
            return new HikariDataSource(storageHikariConfig);
        }
}
