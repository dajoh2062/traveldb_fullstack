package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(50)
public class NewZealandNzetaRule implements Rule {

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        if (!ctx.touchesCountry("NZ")) return;

        if (!"NZ".equalsIgnoreCase(ctx.nationality())) {
            result.addDocument(
                    "NZ_NZETA_OR_TRANSIT_VISA (DEPENDS ON NATIONALITY)"
            );
            result.addAssumption(
                    "New Zealand transit requirements vary by passport."
            );
        }
    }
}
