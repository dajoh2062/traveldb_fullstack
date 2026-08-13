# Baggage transfer rules

For journeys with checked baggage, the baggage engine returns one result for each connection airport.

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

Last source review: **2026-07-31**.

When changing a rule, update these together:

- `BaggageService` for the condition, result text, source, and review date; and
- `BaggageRulesIntegrationTests` for route-level coverage.

Run `.\mvnw.cmd test` from `traveldbBackend` after making a change.
