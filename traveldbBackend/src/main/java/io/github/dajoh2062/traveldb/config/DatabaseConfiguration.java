package io.github.dajoh2062.traveldb.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
public class DatabaseConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) {
        properties.setUrl(toJdbcUrl(properties.getUrl()));
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    static String toJdbcUrl(String configuredUrl) {
        if (configuredUrl == null
                || configuredUrl.startsWith("jdbc:")
                || !(configuredUrl.startsWith("postgres://") || configuredUrl.startsWith("postgresql://"))) {
            return configuredUrl;
        }

        URI uri = URI.create(configuredUrl);
        if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() < 2) {
            throw new IllegalArgumentException("PostgreSQL database URL must include a host and database name.");
        }
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query;
    }
}
