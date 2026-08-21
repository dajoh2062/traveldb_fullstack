# Baggage transfer rules

For journeys with checked baggage, the baggage engine returns one result for each connection airport.

The active baggage-rule dataset is stored in relational tables. `BaggageRuleRepository` loads the version, review date, airport groups, ordered selectors, output text, exceptions, and sources. `BaggageService` caches that immutable dataset, while the pure `BaggageRuleMatcher` evaluates each connection.

| Status | Meaning |
| --- | --- |
| `REQUIRED` | A matching rule says the traveller must collect and recheck the bag. |
| `CONFIRM` | The answer depends on the airline, airport, origin, or baggage tag. |
| `NOT_REQUIRED` | No supported reclaim rule matched and the traveller said the bag is checked through. |

The baggage tag and instructions from airline, airport, customs, and border staff take precedence over these results.

## How a result is chosen

The engine considers:

- whether the traveller has checked baggage;
- whether the flights are on one booking or separate tickets;
- whether the bag is tagged to the final destination;
- whether an international arrival connects to a domestic flight; and
- whether a US-bound flight leaves from a CBP preclearance airport.

Connections are evaluated in itinerary order. Border-handling rules take priority, followed by the baggage-tag answer and ticket arrangement. When a supported rule cannot give a reliable answer, the result is `CONFIRM`.

Rules are declarative and ordered by priority, then by their stable position. A rule can select on:

- entering a new country;
- continuing on a domestic flight;
- current country or airport;
- previous airport or a named airport group;
- ticket arrangement; and
- through-check status.

Route traversal and the calculation of entry/domestic facts remain Java logic. The policy conditions, messages, exceptions, sources, airport-group membership, and source-review date are database data.

## Supported cases

| Journey condition | Result | Source |
| --- | --- | --- |
| Overseas arrival in the United States with an onward flight | Collect at the first US arrival | [US CBP baggage guidance](https://www.help.cbp.gov/s/article/Article-1244?language=en_US) |
| US-bound flight from a CBP preclearance airport | The usual US reclaim step is removed; the baggage tag still matters | [US CBP preclearance](https://www.help.cbp.gov/s/article/Article-1333?language=en_US) |
| American Airlines Sydney-Los Angeles screening pilot | Possible no-recheck exception unless referred by CBP | [US CBP remote baggage screening](https://www.help.cbp.gov/s/article/Article-1913?language=en_US) |
| International arrival connecting to an Australian domestic flight | Collect, including when travelling on one ticket | [Qantas connection guidance](https://www.qantas.com/en-au/at-the-airport/flight-connections/perth) |
| International arrival connecting to a New Zealand domestic flight | Collect for Customs and biosecurity | [Auckland Airport transfer guidance](https://www.aucklandairport.co.nz/information/directions-between-terminals) |
| International arrival connecting to a Japan domestic flight | Collect for Customs | [ANA connection guidance](https://www.ana.co.jp/en/jp/guide/boarding-procedures/checkin/international/notice/) |
| International arrival connecting domestically at Delhi | Collect even when the bag is tagged through | [Air India Delhi guidance](https://www.airindia.com/in/en/newsroom/articles/Air-India-terminal-changes-at-Delhi-Airport-here-is-everything-you-need-to-know.html) |
| International arrival connecting domestically in Canada | Confirm; airport, origin, and airline programs differ | [Air Canada Toronto guidance](https://www.aircanada.com/ca/en/aco/home/fly/at-the-airport/airport-information/toronto-pearson-international-airport/int-ca.html) |
| Separate tickets without confirmed through-checking | Normally collect at each connection | [British Airways separate-ticket guidance](https://www.britishairways.com/travel/helpcentre/public/en_gb/faq/content/baggage/travelling-on-separate-tickets) |

## Maintenance

Active seed dataset: **2026-07-31.1**. Last source review: **2026-07-31**.

Flyway migrations `V3` and `V4` create and seed the initial baggage dataset. Do not edit an already-deployed migration. To publish a change:

1. Add a new migration that inserts a new `baggage_rule_datasets` version and all of its rules and supporting records.
2. In that migration, change the single `active_baggage_rule_dataset` row only after the new dataset is complete.
3. Update `BaggageRulesIntegrationTests` and repository tests for route-level and data-integrity coverage.
4. Review the complete SQL diff, including source URLs and the dataset review date.

Run `.\mvnw.cmd test` from `traveldbBackend` after making a change.
