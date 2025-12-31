package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.Airport;
import projects.traveldbbackend.TravelRepository;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(10)
public class SchengenDestinationRule implements Rule {

    private final TravelRepository repo;

    public SchengenDestinationRule(TravelRepository repo) {
        this.repo = repo;
    }

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        Airport dest = ctx.destination();
        if (!dest.isSchengen()) return;

        boolean natSchengen = Boolean.TRUE.equals(
                repo.isSchengenCountry(ctx.nationality())
        );

        if (!natSchengen) {
            result.addDocument("SCHENGEN_VISA_OR_RESIDENCE_PERMIT");
            result.addAssumption(
                    "This is simplified; actual Schengen entry rules "
                  + "depend on duration and permits."
            );
        }
    }
}
