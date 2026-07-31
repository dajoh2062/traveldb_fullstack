package projects.traveldbbackend.rules.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import projects.traveldbbackend.model.Airport;
import projects.traveldbbackend.rules.BaggageAdvice;
import projects.traveldbbackend.rules.Rule;
import projects.traveldbbackend.rules.RuleContext;
import projects.traveldbbackend.rules.RuleResult;

import java.util.List;
import java.util.Set;

@Component
@Order(25)
public class BaggageTransferRule implements Rule {

    private static final BaggageAdvice.Source CBP_BAGGAGE = new BaggageAdvice.Source(
            "U.S. Customs and Border Protection — checked baggage",
            "https://www.help.cbp.gov/s/article/Article-1244?language=en_US"
    );
    private static final BaggageAdvice.Source CBP_PRECLEARANCE = new BaggageAdvice.Source(
            "U.S. Customs and Border Protection — preclearance",
            "https://www.help.cbp.gov/s/article/Article-1333?language=en_US"
    );
    private static final BaggageAdvice.Source CBP_REMOTE_SCREENING = new BaggageAdvice.Source(
            "U.S. Customs and Border Protection — remote baggage screening pilot",
            "https://www.help.cbp.gov/s/article/Article-1913?language=en_US"
    );
    private static final BaggageAdvice.Source SEPARATE_TICKETS = new BaggageAdvice.Source(
            "British Airways — baggage on separate tickets",
            "https://www.britishairways.com/travel/helpcentre/public/en_gb/faq/content/baggage/travelling-on-separate-tickets"
    );
    private static final BaggageAdvice.Source AUSTRALIA_CONNECTIONS = new BaggageAdvice.Source(
            "Qantas — international to domestic connections in Australia",
            "https://www.qantas.com/en-au/at-the-airport/flight-connections/perth"
    );
    private static final BaggageAdvice.Source NEW_ZEALAND_CONNECTIONS = new BaggageAdvice.Source(
            "Auckland Airport — international to domestic transfers",
            "https://www.aucklandairport.co.nz/information/directions-between-terminals"
    );
    private static final BaggageAdvice.Source JAPAN_CONNECTIONS = new BaggageAdvice.Source(
            "ANA — international to domestic through check-in",
            "https://www.ana.co.jp/en/jp/guide/boarding-procedures/checkin/international/notice/"
    );
    private static final BaggageAdvice.Source DELHI_CONNECTIONS = new BaggageAdvice.Source(
            "Air India — international to domestic connections at Delhi",
            "https://www.airindia.com/in/en/newsroom/articles/Air-India-terminal-changes-at-Delhi-Airport-here-is-everything-you-need-to-know.html"
    );
    private static final BaggageAdvice.Source CANADA_CONNECTIONS = new BaggageAdvice.Source(
            "Air Canada — international to Canada connections at Toronto",
            "https://www.aircanada.com/ca/en/aco/home/fly/at-the-airport/airport-information/toronto-pearson-international-airport/int-ca.html"
    );

    private static final Set<String> US_PRECLEARANCE_AIRPORTS = Set.of(
            "DUB", "SNN", "AUA", "BDA", "AUH", "NAS",
            "YYC", "YYZ", "YEG", "YHZ", "YUL", "YOW", "YVR", "YYJ", "YWG"
    );

    @Override
    public void apply(RuleContext ctx, RuleResult result) {
        if (!ctx.hasCheckedBaggage()) {
            result.addNote("No checked baggage was selected, so no baggage reclaim steps apply.");
            return;
        }

        result.addAssumption("Advice applies to checked baggage and the airports entered in this itinerary.");
        result.addNote("Always read the destination printed on the baggage tag issued at check-in.");
        result.addNote("Airline interline agreements, terminal changes, overnight connections and operational instructions can override the general result.");

        for (int i = 1; i < ctx.route().size() - 1; i++) {
            Airport previous = ctx.route().get(i - 1);
            Airport current = ctx.route().get(i);
            Airport next = ctx.route().get(i + 1);
            result.addBaggageAdvice(adviceFor(ctx, previous, current, next));
        }
    }

    private BaggageAdvice adviceFor(RuleContext ctx, Airport previous, Airport current, Airport next) {
        boolean enteringCountry = !sameCountry(previous, current);
        boolean onwardDomestic = sameCountry(current, next);

        if (enteringCountry && "US".equals(current.getCountryCode())) {
            return usEntryAdvice(ctx, previous, current);
        }

        if (enteringCountry && onwardDomestic) {
            if ("IN".equals(current.getCountryCode()) && "DEL".equals(current.getIataCode())) {
                return required(
                        current,
                        "Collect and recheck at Delhi",
                        "International-to-domestic passengers at Delhi must collect checked baggage, clear Customs and use the transfer desk, even when the bag is tagged through.",
                        List.of("New hub-and-spoke processing is being introduced on selected Air India routes; eligible flights may use different procedures."),
                        DELHI_CONNECTIONS
                );
            }

            return switch (current.getCountryCode()) {
                case "AU" -> required(
                        current,
                        "Collect for Australian border clearance",
                        "International arrivals connecting to an Australian domestic flight must collect checked baggage, clear immigration and customs, then recheck it — including on one ticket.",
                        List.of("Special domestic sectors operated as part of an international service can use different processing; follow the operating airline's instructions."),
                        AUSTRALIA_CONNECTIONS
                );
                case "NZ" -> required(
                        current,
                        "Collect for Customs and biosecurity",
                        "On arrival in New Zealand, checked baggage must be collected and cleared before an onward domestic flight.",
                        List.of("International-to-international transfer baggage normally remains in the secure transfer system when it is checked through."),
                        NEW_ZEALAND_CONNECTIONS
                );
                case "JP" -> required(
                        current,
                        "Collect at the first airport in Japan",
                        "For an international arrival connecting to a Japan domestic flight, collect checked baggage for Customs and check it in again.",
                        List.of("Domestic-to-international journeys can usually be through-checked; airport changes such as Haneda–Narita still require handling the bag yourself."),
                        JAPAN_CONNECTIONS
                );
                case "CA" -> confirm(
                        current,
                        "Canadian transfer process varies",
                        "Canadian hubs use airport-, origin- and airline-specific baggage transfer programs. Some passengers clear Customs without collecting bags; others must reclaim them.",
                        List.of("At Toronto, the no-reclaim process only applies to listed origins and eligible connecting itineraries.", "Follow the airline's transfer instructions and the baggage tag."),
                        CANADA_CONNECTIONS
                );
                default -> confirm(
                        current,
                        "Confirm first-port-of-entry handling",
                        "You are arriving internationally and continuing on a domestic flight. Many countries require baggage to be presented at the first point of entry, but the process is country- and airport-specific.",
                        List.of("Through-checking does not always remove a Customs reclaim requirement.", "Check the official airport transfer guide and ask the airline at check-in."),
                        SEPARATE_TICKETS
                );
            };
        }

        if (ctx.throughCheckStatus() == RuleContext.ThroughCheckStatus.NO) {
            return required(
                    current,
                    "Bag is not checked through",
                    "The baggage tag does not cover the onward journey, so collect the bag and check it in again for the next flight.",
                    List.of("If airline staff retag the bag to a later airport, follow the updated baggage tag."),
                    SEPARATE_TICKETS
            );
        }

        if (ctx.throughCheckStatus() == RuleContext.ThroughCheckStatus.YES) {
            return notRequired(
                    current,
                    "No known reclaim requirement",
                    "Your bag is checked through and this connection does not match a supported mandatory Customs reclaim rule.",
                    List.of("Collect it if the airline, airport signs or border officers instruct you to do so."),
                    SEPARATE_TICKETS
            );
        }

        if (ctx.ticketArrangement() == RuleContext.TicketArrangement.SEPARATE_TICKETS) {
            return required(
                    current,
                    "Separate tickets normally require self-transfer",
                    "Separate bookings are separate journeys, so baggage is normally collected and checked in again at the connection.",
                    List.of("An airline may agree to through-check bags across separate tickets; confirm this at the first check-in desk."),
                    SEPARATE_TICKETS
            );
        }

        return confirm(
                current,
                "Check the baggage tag",
                "No supported border rule makes pickup certain here, but transfer depends on whether the airline tags the bag beyond this airport.",
                List.of("One booking often allows through-checking, but airline partnerships and airport procedures still matter."),
                SEPARATE_TICKETS
        );
    }

    private BaggageAdvice usEntryAdvice(RuleContext ctx, Airport previous, Airport current) {
        if (US_PRECLEARANCE_AIRPORTS.contains(previous.getIataCode())) {
            if (ctx.throughCheckStatus() == RuleContext.ThroughCheckStatus.NO) {
                return required(
                        current,
                        "Collect because the bag is not checked through",
                        "U.S. border processing was completed before departure, but the baggage tag does not cover the onward flight.",
                        List.of("If the airline retags the bag through, U.S. preclearance normally lets it transfer without reclaim on arrival."),
                        CBP_PRECLEARANCE
                );
            }
            if (ctx.throughCheckStatus() == RuleContext.ThroughCheckStatus.YES) {
                return notRequired(
                        current,
                        "Precleared before the U.S. flight",
                        "This flight departs from a CBP preclearance airport, so eligible passengers arrive like domestic travellers and a through-checked bag normally transfers onward.",
                        List.of("Follow any airline or CBP instruction to collect a bag selected for inspection."),
                        CBP_PRECLEARANCE
                );
            }
            return confirm(
                    current,
                    "Preclearance removes the usual U.S. reclaim step",
                    "CBP processing happens before departure from this airport. Confirm that the baggage tag covers the onward flight.",
                    List.of("A bag not tagged through still has to be collected and checked in again."),
                    CBP_PRECLEARANCE
            );
        }

        if ("SYD".equals(previous.getIataCode()) && "LAX".equals(current.getIataCode())) {
            return required(
                    current,
                    "Collect unless your flight uses the CBP screening pilot",
                    "U.S. arrivals normally reclaim checked baggage for CBP. A route-specific remote-screening pilot may transfer eligible bags on the American Airlines Sydney–Los Angeles service.",
                    List.of(
                            "Confirm pilot eligibility with American Airlines; collect the bag if CBP refers it for inspection.",
                            "CBP preclearance flights bypass the usual U.S. arrival process."
                    ),
                    CBP_BAGGAGE,
                    CBP_REMOTE_SCREENING,
                    CBP_PRECLEARANCE
            );
        }

        return required(
                current,
                "Collect at the first U.S. arrival",
                "Travellers entering the United States from overseas normally collect checked baggage for CBP and recheck it before any onward flight.",
                List.of("CBP preclearance flights bypass this arrival process when baggage is checked through."),
                CBP_BAGGAGE,
                CBP_PRECLEARANCE
        );
    }

    private boolean sameCountry(Airport first, Airport second) {
        return first.getCountryCode().equalsIgnoreCase(second.getCountryCode());
    }

    private BaggageAdvice required(
            Airport airport,
            String title,
            String explanation,
            List<String> exceptions,
            BaggageAdvice.Source... sources
    ) {
        return advice(airport, BaggageAdvice.Status.REQUIRED, title, explanation, exceptions, sources);
    }

    private BaggageAdvice confirm(
            Airport airport,
            String title,
            String explanation,
            List<String> exceptions,
            BaggageAdvice.Source... sources
    ) {
        return advice(airport, BaggageAdvice.Status.CONFIRM, title, explanation, exceptions, sources);
    }

    private BaggageAdvice notRequired(
            Airport airport,
            String title,
            String explanation,
            List<String> exceptions,
            BaggageAdvice.Source... sources
    ) {
        return advice(airport, BaggageAdvice.Status.NOT_REQUIRED, title, explanation, exceptions, sources);
    }

    private BaggageAdvice advice(
            Airport airport,
            BaggageAdvice.Status status,
            String title,
            String explanation,
            List<String> exceptions,
            BaggageAdvice.Source... sources
    ) {
        return new BaggageAdvice(
                airport.getIataCode(),
                status,
                title,
                explanation,
                exceptions,
                List.of(sources)
        );
    }
}
