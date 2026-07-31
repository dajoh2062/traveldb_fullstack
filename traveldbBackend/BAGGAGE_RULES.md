# Baggage transfer rules

For journeys with checked baggage, the baggage engine returns one result for each connection airport:

| Status | Meaning |
| --- | --- |
| `REQUIRED` | The matching rule says the traveller must collect and recheck the bag. |
| `CONFIRM` | The result depends on the airline, airport, origin, or baggage tag. |
| `NOT_REQUIRED` | No supported reclaim rule matched and the traveller said the bag is checked through. |

These results are guidance. The baggage tag and instructions from airline, airport, customs, and border staff take precedence.

## Inputs

The result depends on:

- whether the traveller has checked baggage;
- whether flights are on one booking or separate tickets;
- whether the baggage tag covers the final destination;
- whether an international arrival connects to a domestic flight; and
- whether a U.S.-bound flight leaves from a CBP preclearance airport.

`BaggageTransferRule` evaluates intermediate airports in itinerary order. Explicit border-handling rules take priority, followed by the stated baggage-tag status and ticket arrangement. An unsupported international-to-domestic transfer returns `CONFIRM` rather than guessing.

## Supported cases

| Journey condition | Result | Source |
| --- | --- | --- |
| Overseas arrival in the United States with an onward flight | Collect at the first U.S. arrival | [U.S. CBP baggage guidance](https://www.help.cbp.gov/s/article/Article-1244?language=en_US) |
| U.S.-bound flight from a CBP preclearance airport | The usual U.S. reclaim step is removed; the baggage tag still matters | [U.S. CBP preclearance](https://www.help.cbp.gov/s/article/Article-1333?language=en_US) |
| American Airlines Sydney-Los Angeles screening pilot | Possible no-recheck exception unless referred by CBP | [U.S. CBP remote baggage screening](https://www.help.cbp.gov/s/article/Article-1913?language=en_US) |
| International arrival connecting to an Australian domestic flight | Collect, including when travelling on one ticket | [Qantas connection guidance](https://www.qantas.com/en-au/at-the-airport/flight-connections/perth) |
| International arrival connecting to a New Zealand domestic flight | Collect for Customs and biosecurity | [Auckland Airport transfer guidance](https://www.aucklandairport.co.nz/information/directions-between-terminals) |
| International arrival connecting to a Japan domestic flight | Collect for Customs | [ANA connection guidance](https://www.ana.co.jp/en/jp/guide/boarding-procedures/checkin/international/notice/) |
| International arrival connecting domestically at Delhi | Collect even when the bag is tagged through | [Air India Delhi guidance](https://www.airindia.com/in/en/newsroom/articles/Air-India-terminal-changes-at-Delhi-Airport-here-is-everything-you-need-to-know.html) |
| International arrival connecting domestically in Canada | Confirm; airport, origin, and airline programs differ | [Air Canada Toronto guidance](https://www.aircanada.com/ca/en/aco/home/fly/at-the-airport/airport-information/toronto-pearson-international-airport/int-ca.html) |
| Separate tickets without confirmed through-checking | Normally collect at each connection | [British Airways separate-ticket guidance](https://www.britishairways.com/travel/helpcentre/public/en_gb/faq/content/baggage/travelling-on-separate-tickets) |

## Maintenance

The guidance was last reviewed on **2026-07-20**. A rule update normally touches these files together:

- `src/main/java/projects/traveldbbackend/rules/impl/BaggageTransferRule.java` for conditions, wording, and sources;
- `src/test/java/projects/traveldbbackend/BaggageRulesIntegrationTests.java` for journey-level regression coverage; and
- `src/main/java/projects/traveldbbackend/service/TravelService.java` for the response's `baggageGuidanceReviewed` date.

Run `.\mvnw.cmd test` from `traveldbBackend` after changing a rule.
