package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(30)
public class UkEtaRule implements Rule {

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        if (!ctx.touchesCountry("GB")) return;

        // Real eligibility depends on nationality and transit type.
        // We mark this as conditional until you add data tables.
        if (!"GB".equalsIgnoreCase(ctx.nationality())
                && !"IE".equalsIgnoreCase(ctx.nationality())) {
            result.addDocument(
                    "UK_ETA_OR_UK_VISA (DEPENDS ON NATIONALITY / TRANSIT)"
            );
            result.addAssumption(
                    "UK requirements depend on whether you pass border "
                  + "control (airside vs landside)."
            );
        }
    }
}
