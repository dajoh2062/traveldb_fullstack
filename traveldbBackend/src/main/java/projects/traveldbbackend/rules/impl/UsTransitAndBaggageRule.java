package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.Airport;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

@Component
@Order(20)
public class UsTransitAndBaggageRule implements Rule {

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        boolean touchesUs = ctx.touchesCountry("US");
        if (!touchesUs) return;

        if (!"US".equalsIgnoreCase(ctx.nationality())) {
            result.addDocument("US_ENTRY_PERMISSION (ESTA / VISA / GREENCARD)");
            result.addAssumption(
                    "US transit usually requires entry clearance. "
                  + "Airside-only transit is uncommon."
            );
        }

        // Add every entry into US that is not the final airport.
        // If you enter, leave, then re-enter: you get multiple pickups.
        for (int i = 1; i < ctx.route().size(); i++) {
            Airport prev = ctx.route().get(i - 1);
            Airport cur = ctx.route().get(i);

            boolean enteringUs = !"US".equalsIgnoreCase(prev.getCountryCode())
                    && "US".equalsIgnoreCase(cur.getCountryCode());

            boolean isFinalStop = (i == ctx.route().size() - 1);

            if (enteringUs && !isFinalStop) {
                result.addBaggagePickupAt(cur.getIataCode());
                result.addNote(
                        "Assuming baggage reclaim + recheck on US entry."
                );
            }
        }
    }
}
