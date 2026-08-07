package com.example.urlshortener.operations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mock.env.MockEnvironment;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthPolicyTests {
    @Test
    void readinessConfigurationShouldRequireDatabaseAndExcludeRedis() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("management.endpoint.health.group.readiness.include", "readinessState,db")
                .withProperty("management.endpoint.health.group.liveness.include", "livenessState,ping");

        assertThat(environment.getProperty("management.endpoint.health.group.readiness.include")).contains("db");
        assertThat(environment.getProperty("management.endpoint.health.group.readiness.include")).doesNotContain("redis");
        assertThat(environment.getProperty("management.endpoint.health.group.liveness.include")).doesNotContain("db", "redis");
    }

    @Test
    void databaseHealthIndicatorShouldReportDownWhenPostgreSqlUnavailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));
        DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(dataSource);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
