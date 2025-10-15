package com.pkg.jpa;

import com.pkg.config.StorageDataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(StorageDataSourceConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseConnectionTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("storage.datasource 커넥션 풀로 DB 연결 및 SELECT 1 확인")
    void connection_and_select1() throws Exception {
        // 커넥션 열기
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }

        // SELECT 1
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);

        String currentDb = jdbc.queryForObject("SELECT DATABASE()", String.class);
        assertThat(currentDb).isEqualTo("little-writer-v2");

        // (옵션) Hikari 풀 정보 점검
        if (dataSource instanceof HikariDataSource hikari) {
            assertThat(hikari.getPoolName()).isNotBlank();
             System.out.println("Pool=" + hikari.getPoolName() + ", URL=" + hikari.getJdbcUrl());
        }
    }
}
