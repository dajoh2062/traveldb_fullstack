package projects.traveldbbackend.rules;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public RuleResult evaluate(RuleContext context) {
        RuleResult result = new RuleResult();
        for (Rule rule : rules) {
            rule.apply(context, result);
        }
        return result;
    }
}
