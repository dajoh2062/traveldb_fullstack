package io.github.dajoh2062.traveldb.bootstrap;

import io.github.dajoh2062.traveldb.service.ReferenceDataImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataBootstrap implements ApplicationRunner {

    private final ReferenceDataImportService importService;
    private final boolean enabled;

    public ReferenceDataBootstrap(
            ReferenceDataImportService importService,
            @Value("${traveldb.reference-data.bootstrap-enabled:true}") boolean enabled
    ) {
        this.importService = importService;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (enabled) {
            importService.importMissingReferenceData();
        }
    }
}
