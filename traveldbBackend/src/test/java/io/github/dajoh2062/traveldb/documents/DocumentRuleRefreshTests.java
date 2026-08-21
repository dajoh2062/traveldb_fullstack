package io.github.dajoh2062.traveldb.documents;

import io.github.dajoh2062.traveldb.support.TestAirports;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentRuleRefreshTests {

    @Test
    void reloadsRulesWhenTheActiveDatasetChanges() {
        DocumentRuleRepository repository = mock(DocumentRuleRepository.class);
        DocumentRuleSnapshot first = new DocumentRuleSnapshot("v1", Instant.EPOCH, List.of(), List.of());
        DocumentRuleSnapshot second = new DocumentRuleSnapshot("v2", Instant.EPOCH, List.of(), List.of());
        when(repository.findActiveDatasetVersion())
                .thenReturn(Optional.of("v1"))
                .thenReturn(Optional.of("v1"))
                .thenReturn(Optional.of("v2"))
                .thenReturn(Optional.of("v2"));
        when(repository.findActive())
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second));
        LocalDocumentRulesProvider provider = new LocalDocumentRulesProvider(
                new ConservativeDocumentProvider(Clock.systemUTC()),
                repository,
                Clock.systemUTC()
        );
        DocumentCheckInput input = new DocumentCheckInput(
                "NO",
                List.of(TestAirports.airport("AAA", "NO"), TestAirports.airport("BBB", "XY")),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        );

        assertEquals("v1", provider.check(input).datasetVersion());
        assertEquals("v2", provider.check(input).datasetVersion());

        verify(repository, times(2)).findActive();
    }
}
