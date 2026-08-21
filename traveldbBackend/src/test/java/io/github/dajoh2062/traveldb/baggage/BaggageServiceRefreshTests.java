package io.github.dajoh2062.traveldb.baggage;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaggageServiceRefreshTests {

    @Test
    void reloadsRulesWhenTheActiveDatasetChanges() {
        BaggageRuleRepository repository = mock(BaggageRuleRepository.class);
        BaggageRuleSnapshot first = new BaggageRuleSnapshot(
                "v1", LocalDate.of(2026, 1, 1), Map.of(), List.of()
        );
        BaggageRuleSnapshot second = new BaggageRuleSnapshot(
                "v2", LocalDate.of(2026, 2, 1), Map.of(), List.of()
        );
        when(repository.findActiveDatasetVersion())
                .thenReturn(Optional.of("v1"))
                .thenReturn(Optional.of("v1"))
                .thenReturn(Optional.of("v2"))
                .thenReturn(Optional.of("v2"));
        when(repository.findActive())
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second));
        BaggageService service = new BaggageService(repository, mock(BaggageRuleMatcher.class));
        BaggageCheckRequest request = new BaggageCheckRequest(
                List.of(),
                false,
                BaggageCheckRequest.TicketArrangement.UNKNOWN,
                BaggageCheckRequest.ThroughCheckStatus.UNKNOWN
        );

        assertEquals("2026-01-01", service.check(request).guidanceReviewedDate());
        assertEquals("2026-02-01", service.check(request).guidanceReviewedDate());

        verify(repository, times(2)).findActive();
    }
}
