# Baggage transfer rules

The journey checker deliberately separates three outcomes:

- `REQUIRED`: an official source describes a reclaim/recheck step for the matching journey.
- `CONFIRM`: the answer depends on the airline, origin, airport transfer program, or baggage tag.
- `NOT_REQUIRED`: no supported customs reclaim rule matched and the traveller said the bag is tagged through.

The response is guidance, not a guarantee. The baggage tag and instructions from the operating airline, airport, and border officers take precedence.

## Inputs that affect the result

- Whether the traveller has checked baggage.
- Whether all flights are on one booking or separate bookings.
- Whether the issued baggage tag names the final destination or the connection airport.
- Whether the connection changes from an international arrival to a domestic departure.
- Whether the traveller enters the United States at a CBP preclearance origin.

## Rules with explicit source support

| Journey condition | Result | Primary guidance |
| --- | --- | --- |
| Overseas arrival in the United States with an onward flight | Required at the first U.S. arrival | [U.S. CBP baggage guidance](https://www.help.cbp.gov/s/article/Article-1244?language=en_US) |
| U.S.-bound flight from a CBP preclearance airport | Usual U.S. reclaim step removed; baggage tag still matters | [U.S. CBP preclearance](https://www.help.cbp.gov/s/article/Article-1333?language=en_US) |
| American Airlines Sydney–Los Angeles pilot | Possible no-recheck exception unless referred by CBP | [U.S. CBP remote baggage screening](https://www.help.cbp.gov/s/article/Article-1913?language=en_US) |
| International arrival connecting to an Australian domestic flight | Required, including on one ticket | [Qantas connection guidance](https://www.qantas.com/en-au/at-the-airport/flight-connections/perth) |
| International arrival connecting to a New Zealand domestic flight | Required for Customs and biosecurity | [Auckland Airport transfer guidance](https://www.aucklandairport.co.nz/information/directions-between-terminals) |
| International arrival connecting to a Japan domestic flight | Required for Customs | [ANA connection guidance](https://www.ana.co.jp/en/jp/guide/boarding-procedures/checkin/international/notice/) |
| International arrival connecting domestically at Delhi | Required even if tagged through | [Air India Delhi guidance](https://www.airindia.com/in/en/newsroom/articles/Air-India-terminal-changes-at-Delhi-Airport-here-is-everything-you-need-to-know.html) |
| International arrival connecting domestically in Canada | Conditional; airport, origin and airline programs differ | [Air Canada Toronto guidance](https://www.aircanada.com/ca/en/aco/home/fly/at-the-airport/airport-information/toronto-pearson-international-airport/int-ca.html) |
| Separate tickets without confirmed through-checking | Normally required at every connection | [British Airways separate-ticket guidance](https://www.britishairways.com/travel/helpcentre/public/en_gb/faq/content/baggage/travelling-on-separate-tickets) |

## Maintenance

Official guidance was last reviewed on **2026-07-20**. When a rule changes, update the rule text, source URL, review date in `TravelService`, and its regression test together.
