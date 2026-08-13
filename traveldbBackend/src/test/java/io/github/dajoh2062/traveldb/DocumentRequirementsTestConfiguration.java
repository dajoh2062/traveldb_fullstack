package io.github.dajoh2062.traveldb;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@TestConfiguration(proxyBeanMethods = false)
class DocumentRequirementsTestConfiguration {

    @Bean
    @Primary
    Clock testClock() {
        return DocumentRequirementsIntegrationTestSupport.TEST_CLOCK;
    }
}
