package io.github.dajoh2062.traveldb.service;

import io.github.dajoh2062.traveldb.model.Airport;
import io.github.dajoh2062.traveldb.repository.AirportRepository;
import io.github.dajoh2062.traveldb.support.TestAirports;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AirportSearchServiceRefreshTests {

    @Test
    void reloadsTheSearchIndexAfterItsTtlExpires() {
        AirportRepository repository = mock(AirportRepository.class);
        Airport first = TestAirports.airport("AAA", "NO");
        Airport second = TestAirports.airport("BBB", "NO");
        when(repository.findAll())
                .thenReturn(List.of(first))
                .thenReturn(List.of(second));
        MutableClock clock = new MutableClock(Instant.parse("2026-08-21T12:00:00Z"));
        AirportSearchService service = new AirportSearchService(repository, clock, Duration.ofMinutes(5));

        assertEquals("AAA", service.rankedMatches("AAA").getFirst().iataCode());
        clock.advance(Duration.ofMinutes(6));
        assertEquals("BBB", service.rankedMatches("BBB").getFirst().iataCode());

        verify(repository, times(2)).findAll();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
