package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(1)
public class PassportRule implements Rule {

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        result.addDocument("PASSPORT");
    }
}
