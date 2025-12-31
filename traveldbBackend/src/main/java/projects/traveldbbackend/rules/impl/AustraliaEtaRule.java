package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(40)
public class AustraliaEtaRule implements Rule {

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        if (!ctx.touchesCountry("AU")) return;

        if (!"AU".equalsIgnoreCase(ctx.nationality())) {
            result.addDocument(
                    "AU_ETA_OR_AU_VISA (DEPENDS ON NATIONALITY / TRANSIT)"
            );
            result.addAssumption(
                    "Australia ETA eligibility varies by passport."
            );
        }
    }
}
