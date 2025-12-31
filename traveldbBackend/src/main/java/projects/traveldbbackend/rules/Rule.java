package projects.traveldbbackend.rules;

public interface Rule {
    void apply(RuleContext ctx, RuleResult result);
}
