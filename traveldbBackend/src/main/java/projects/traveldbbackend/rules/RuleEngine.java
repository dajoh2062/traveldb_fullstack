package projects.traveldbbackend.rules;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        // Spring injects all Rule beans.
        // We keep their @Order.
        this.rules = rules;
    }

    public RuleResult evaluate(RuleContext ctx) {
        RuleResult result = new RuleResult();

        for (Rule r : rules) {
            r.apply(ctx, result);
        }

        return result;
    }
}
