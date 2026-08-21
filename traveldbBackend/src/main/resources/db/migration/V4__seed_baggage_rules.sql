INSERT INTO baggage_rule_datasets (dataset_version, reviewed_date)
VALUES ('2026-07-31.1', DATE '2026-07-31');

INSERT INTO active_baggage_rule_dataset (slot, dataset_version)
VALUES (1, '2026-07-31.1');

INSERT INTO baggage_sources (dataset_version, source_key, label, url) VALUES
('2026-07-31.1', 'CBP_BAGGAGE', 'U.S. Customs and Border Protection — checked baggage', 'https://www.help.cbp.gov/s/article/Article-1244?language=en_US'),
('2026-07-31.1', 'CBP_PRECLEARANCE', 'U.S. Customs and Border Protection — preclearance', 'https://www.help.cbp.gov/s/article/Article-1333?language=en_US'),
('2026-07-31.1', 'CBP_REMOTE_SCREENING', 'U.S. Customs and Border Protection — remote baggage screening pilot', 'https://www.help.cbp.gov/s/article/Article-1913?language=en_US'),
('2026-07-31.1', 'SEPARATE_TICKETS', 'British Airways — baggage on separate tickets', 'https://www.britishairways.com/travel/helpcentre/public/en_gb/faq/content/baggage/travelling-on-separate-tickets'),
('2026-07-31.1', 'AUSTRALIA_CONNECTIONS', 'Qantas — international to domestic connections in Australia', 'https://www.qantas.com/en-au/at-the-airport/flight-connections/perth'),
('2026-07-31.1', 'NEW_ZEALAND_CONNECTIONS', 'Auckland Airport — international to domestic transfers', 'https://www.aucklandairport.co.nz/information/directions-between-terminals'),
('2026-07-31.1', 'JAPAN_CONNECTIONS', 'ANA — international to domestic through check-in', 'https://www.ana.co.jp/en/jp/guide/boarding-procedures/checkin/international/notice/'),
('2026-07-31.1', 'DELHI_CONNECTIONS', 'Air India — international to domestic connections at Delhi', 'https://www.airindia.com/in/en/newsroom/articles/Air-India-terminal-changes-at-Delhi-Airport-here-is-everything-you-need-to-know.html'),
('2026-07-31.1', 'CANADA_CONNECTIONS', 'Air Canada — international to Canada connections at Toronto', 'https://www.aircanada.com/ca/en/aco/home/fly/at-the-airport/airport-information/toronto-pearson-international-airport/int-ca.html');

INSERT INTO baggage_airport_group_members (dataset_version, group_code, airport_code) VALUES
('2026-07-31.1', 'US_PRECLEARANCE', 'DUB'),
('2026-07-31.1', 'US_PRECLEARANCE', 'SNN'),
('2026-07-31.1', 'US_PRECLEARANCE', 'AUA'),
('2026-07-31.1', 'US_PRECLEARANCE', 'BDA'),
('2026-07-31.1', 'US_PRECLEARANCE', 'AUH'),
('2026-07-31.1', 'US_PRECLEARANCE', 'NAS'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YYC'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YYZ'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YEG'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YHZ'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YUL'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YOW'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YVR'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YYJ'),
('2026-07-31.1', 'US_PRECLEARANCE', 'YWG');

INSERT INTO baggage_rules (
    dataset_version, rule_id, rule_position, priority, entering_country, onward_domestic,
    current_country_code, current_airport_code, previous_airport_code, previous_airport_group,
    ticket_arrangement, through_check_status, advice_code, advice_status, title, explanation
) VALUES
('2026-07-31.1', 'us-preclearance-not-through', 0, 1000, TRUE, NULL, 'US', NULL, NULL, 'US_PRECLEARANCE', NULL, 'NO', 'US_PRECLEARANCE_NOT_CHECKED_THROUGH', 'REQUIRED', 'Collect because the bag is not checked through', 'U.S. border processing was completed before departure, but the baggage tag does not cover the onward flight.'),
('2026-07-31.1', 'us-preclearance-checked-through', 1, 1000, TRUE, NULL, 'US', NULL, NULL, 'US_PRECLEARANCE', NULL, 'YES', 'US_PRECLEARANCE_CHECKED_THROUGH', 'NOT_REQUIRED', 'Precleared before the U.S. flight', 'This flight departs from a CBP preclearance airport, so eligible passengers arrive like domestic travellers and a through-checked bag normally transfers onward.'),
('2026-07-31.1', 'us-preclearance-confirm-tag', 2, 1000, TRUE, NULL, 'US', NULL, NULL, 'US_PRECLEARANCE', NULL, 'UNKNOWN', 'US_PRECLEARANCE_CONFIRM_TAG', 'CONFIRM', 'Preclearance removes the usual U.S. reclaim step', 'CBP processing happens before departure from this airport. Confirm that the baggage tag covers the onward flight.'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 3, 900, TRUE, NULL, 'US', 'LAX', 'SYD', NULL, NULL, NULL, 'US_SYD_LAX_SCREENING_PILOT', 'REQUIRED', 'Collect unless your flight uses the CBP screening pilot', 'U.S. arrivals normally reclaim checked baggage for CBP. A route-specific remote-screening pilot may transfer eligible bags on the American Airlines Sydney–Los Angeles service.'),
('2026-07-31.1', 'us-first-arrival', 4, 800, TRUE, NULL, 'US', NULL, NULL, NULL, NULL, NULL, 'US_FIRST_ARRIVAL', 'REQUIRED', 'Collect at the first U.S. arrival', 'Travellers entering the United States from overseas normally collect checked baggage for CBP and recheck it before any onward flight.'),
('2026-07-31.1', 'delhi-international-to-domestic', 5, 700, TRUE, TRUE, 'IN', 'DEL', NULL, NULL, NULL, NULL, 'DELHI_INTERNATIONAL_TO_DOMESTIC', 'REQUIRED', 'Collect and recheck at Delhi', 'International-to-domestic passengers at Delhi must collect checked baggage, clear Customs and use the transfer desk, even when the bag is tagged through.'),
('2026-07-31.1', 'australia-international-to-domestic', 6, 650, TRUE, TRUE, 'AU', NULL, NULL, NULL, NULL, NULL, 'AUSTRALIA_INTERNATIONAL_TO_DOMESTIC', 'REQUIRED', 'Collect for Australian border clearance', 'International arrivals connecting to an Australian domestic flight must collect checked baggage, clear immigration and customs, then recheck it — including on one ticket.'),
('2026-07-31.1', 'new-zealand-international-to-domestic', 7, 650, TRUE, TRUE, 'NZ', NULL, NULL, NULL, NULL, NULL, 'NEW_ZEALAND_INTERNATIONAL_TO_DOMESTIC', 'REQUIRED', 'Collect for Customs and biosecurity', 'On arrival in New Zealand, checked baggage must be collected and cleared before an onward domestic flight.'),
('2026-07-31.1', 'japan-international-to-domestic', 8, 650, TRUE, TRUE, 'JP', NULL, NULL, NULL, NULL, NULL, 'JAPAN_INTERNATIONAL_TO_DOMESTIC', 'REQUIRED', 'Collect at the first airport in Japan', 'For an international arrival connecting to a Japan domestic flight, collect checked baggage for Customs and check it in again.'),
('2026-07-31.1', 'canada-international-to-domestic', 9, 650, TRUE, TRUE, 'CA', NULL, NULL, NULL, NULL, NULL, 'CANADA_INTERNATIONAL_TO_DOMESTIC', 'CONFIRM', 'Canadian transfer process varies', 'Canadian hubs use airport-, origin- and airline-specific baggage transfer programs. Some passengers clear Customs without collecting bags; others must reclaim them.'),
('2026-07-31.1', 'generic-international-to-domestic', 10, 600, TRUE, TRUE, NULL, NULL, NULL, NULL, NULL, NULL, 'GENERIC_INTERNATIONAL_TO_DOMESTIC', 'CONFIRM', 'Confirm first-port-of-entry handling', 'You are arriving internationally and continuing on a domestic flight. Many countries require baggage to be presented at the first point of entry, but the process is country- and airport-specific.'),
('2026-07-31.1', 'not-checked-through', 11, 500, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'NO', 'NOT_CHECKED_THROUGH', 'REQUIRED', 'Bag is not checked through', 'The baggage tag does not cover the onward journey, so collect the bag and check it in again for the next flight.'),
('2026-07-31.1', 'checked-through-no-known-reclaim', 12, 500, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'YES', 'CHECKED_THROUGH_NO_KNOWN_RECLAIM', 'NOT_REQUIRED', 'No known reclaim requirement', 'Your bag is checked through and this connection does not match a supported mandatory Customs reclaim rule.'),
('2026-07-31.1', 'separate-tickets', 13, 400, NULL, NULL, NULL, NULL, NULL, NULL, 'SEPARATE_TICKETS', NULL, 'SEPARATE_TICKETS', 'REQUIRED', 'Separate tickets normally require self-transfer', 'Separate bookings are separate journeys, so baggage is normally collected and checked in again at the connection.'),
('2026-07-31.1', 'check-baggage-tag', 14, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CHECK_BAGGAGE_TAG', 'CONFIRM', 'Check the baggage tag', 'No supported border rule makes pickup certain here, but transfer depends on whether the airline tags the bag beyond this airport.');

INSERT INTO baggage_rule_exceptions (dataset_version, rule_id, exception_position, exception_text) VALUES
('2026-07-31.1', 'us-preclearance-not-through', 0, 'If the airline retags the bag through, U.S. preclearance normally lets it transfer without reclaim on arrival.'),
('2026-07-31.1', 'us-preclearance-checked-through', 0, 'Follow any airline or CBP instruction to collect a bag selected for inspection.'),
('2026-07-31.1', 'us-preclearance-confirm-tag', 0, 'A bag not tagged through still has to be collected and checked in again.'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 0, 'Confirm pilot eligibility with American Airlines; collect the bag if CBP refers it for inspection.'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 1, 'CBP preclearance flights bypass the usual U.S. arrival process.'),
('2026-07-31.1', 'us-first-arrival', 0, 'CBP preclearance flights bypass this arrival process when baggage is checked through.'),
('2026-07-31.1', 'delhi-international-to-domestic', 0, 'New hub-and-spoke processing is being introduced on selected Air India routes; eligible flights may use different procedures.'),
('2026-07-31.1', 'australia-international-to-domestic', 0, 'Special domestic sectors operated as part of an international service can use different processing; follow the operating airline''s instructions.'),
('2026-07-31.1', 'new-zealand-international-to-domestic', 0, 'International-to-international transfer baggage normally remains in the secure transfer system when it is checked through.'),
('2026-07-31.1', 'japan-international-to-domestic', 0, 'Domestic-to-international journeys can usually be through-checked; airport changes such as Haneda–Narita still require handling the bag yourself.'),
('2026-07-31.1', 'canada-international-to-domestic', 0, 'At Toronto, the no-reclaim process only applies to listed origins and eligible connecting itineraries.'),
('2026-07-31.1', 'canada-international-to-domestic', 1, 'Follow the airline''s transfer instructions and the baggage tag.'),
('2026-07-31.1', 'generic-international-to-domestic', 0, 'Through-checking does not always remove a Customs reclaim requirement.'),
('2026-07-31.1', 'generic-international-to-domestic', 1, 'Check the official airport transfer guide and ask the airline at check-in.'),
('2026-07-31.1', 'not-checked-through', 0, 'If airline staff retag the bag to a later airport, follow the updated baggage tag.'),
('2026-07-31.1', 'checked-through-no-known-reclaim', 0, 'Collect it if the airline, airport signs or border officers instruct you to do so.'),
('2026-07-31.1', 'separate-tickets', 0, 'An airline may agree to through-check bags across separate tickets; confirm this at the first check-in desk.'),
('2026-07-31.1', 'check-baggage-tag', 0, 'One booking often allows through-checking, but airline partnerships and airport procedures still matter.');

INSERT INTO baggage_rule_sources (dataset_version, rule_id, source_position, source_key) VALUES
('2026-07-31.1', 'us-preclearance-not-through', 0, 'CBP_PRECLEARANCE'),
('2026-07-31.1', 'us-preclearance-checked-through', 0, 'CBP_PRECLEARANCE'),
('2026-07-31.1', 'us-preclearance-confirm-tag', 0, 'CBP_PRECLEARANCE'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 0, 'CBP_BAGGAGE'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 1, 'CBP_REMOTE_SCREENING'),
('2026-07-31.1', 'us-syd-lax-screening-pilot', 2, 'CBP_PRECLEARANCE'),
('2026-07-31.1', 'us-first-arrival', 0, 'CBP_BAGGAGE'),
('2026-07-31.1', 'us-first-arrival', 1, 'CBP_PRECLEARANCE'),
('2026-07-31.1', 'delhi-international-to-domestic', 0, 'DELHI_CONNECTIONS'),
('2026-07-31.1', 'australia-international-to-domestic', 0, 'AUSTRALIA_CONNECTIONS'),
('2026-07-31.1', 'new-zealand-international-to-domestic', 0, 'NEW_ZEALAND_CONNECTIONS'),
('2026-07-31.1', 'japan-international-to-domestic', 0, 'JAPAN_CONNECTIONS'),
('2026-07-31.1', 'canada-international-to-domestic', 0, 'CANADA_CONNECTIONS'),
('2026-07-31.1', 'generic-international-to-domestic', 0, 'SEPARATE_TICKETS'),
('2026-07-31.1', 'not-checked-through', 0, 'SEPARATE_TICKETS'),
('2026-07-31.1', 'checked-through-no-known-reclaim', 0, 'SEPARATE_TICKETS'),
('2026-07-31.1', 'separate-tickets', 0, 'SEPARATE_TICKETS'),
('2026-07-31.1', 'check-baggage-tag', 0, 'SEPARATE_TICKETS');
