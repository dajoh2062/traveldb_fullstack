import { mkdir, readFile, writeFile } from "node:fs/promises";

const snapshotPath = "../traveldbBackend/src/main/resources/data/document-rules.json";
const snapshot = JSON.parse(await readFile(snapshotPath, "utf8"));

const documentRules = Object.fromEntries(
  snapshot.rules.map(rule => [
    rule.id,
    {
      title: rule.output.title,
      summary: rule.output.summary,
      conditions: rule.output.conditions,
      keyFacts: rule.output.keyFacts,
    },
  ]),
);

const conservativeRules = {
  "conservative-travel-document": {
    title: "Passport or accepted travel document",
    summary:
      "Confirm which travel document is accepted and its minimum remaining validity. National identity cards can replace passports on some regional journeys.",
    conditions: [
      "Check validity on both the arrival date and planned departure date.",
      "Verify blank-page and document-condition requirements.",
    ],
    keyFacts: [],
  },
  "conservative-transit-permission": {
    title: "Transit permission",
    summary:
      "Transit rules depend on nationality, connection length, airport transfer route and whether border control is crossed.",
    conditions: [
      "Airside and landside connections can have different rules.",
      "Airport or terminal changes usually require entry permission.",
    ],
    keyFacts: [],
  },
  "conservative-entry-permission": {
    title: "Visa, eVisa or electronic authorisation",
    summary:
      "Entry permission depends on nationality, residence, purpose, stay length and documents already held.",
    conditions: [
      "Residence permits and valid visas from other countries can create exemptions.",
      "Visa-free entry can still require an ETA or arrival registration.",
    ],
    keyFacts: [],
  },
};

const baggageAdvices = {
  DELHI_INTERNATIONAL_TO_DOMESTIC: {
    title: "Collect and recheck at Delhi",
    explanation:
      "International-to-domestic passengers at Delhi must collect checked baggage, clear Customs and use the transfer desk, even when the bag is tagged through.",
  },
  AUSTRALIA_INTERNATIONAL_TO_DOMESTIC: {
    title: "Collect for Australian border clearance",
    explanation:
      "International arrivals connecting to an Australian domestic flight must collect checked baggage, clear immigration and customs, then recheck it — including on one ticket.",
  },
  NEW_ZEALAND_INTERNATIONAL_TO_DOMESTIC: {
    title: "Collect for Customs and biosecurity",
    explanation:
      "On arrival in New Zealand, checked baggage must be collected and cleared before an onward domestic flight.",
  },
  JAPAN_INTERNATIONAL_TO_DOMESTIC: {
    title: "Collect at the first airport in Japan",
    explanation:
      "For an international arrival connecting to a Japan domestic flight, collect checked baggage for Customs and check it in again.",
  },
  CANADA_INTERNATIONAL_TO_DOMESTIC: {
    title: "Canadian transfer process varies",
    explanation:
      "Canadian hubs use airport-, origin- and airline-specific baggage transfer programs. Some passengers clear Customs without collecting bags; others must reclaim them.",
  },
  GENERIC_INTERNATIONAL_TO_DOMESTIC: {
    title: "Confirm first-port-of-entry handling",
    explanation:
      "You are arriving internationally and continuing on a domestic flight. Many countries require baggage to be presented at the first point of entry, but the process is country- and airport-specific.",
  },
  NOT_CHECKED_THROUGH: {
    title: "Bag is not checked through",
    explanation:
      "The baggage tag does not cover the onward journey, so collect the bag and check it in again for the next flight.",
  },
  CHECKED_THROUGH_NO_KNOWN_RECLAIM: {
    title: "No known reclaim requirement",
    explanation:
      "Your bag is checked through and this connection does not match a supported mandatory Customs reclaim rule.",
  },
  SEPARATE_TICKETS: {
    title: "Separate tickets normally require self-transfer",
    explanation:
      "Separate bookings are separate journeys, so baggage is normally collected and checked in again at the connection.",
  },
  CHECK_BAGGAGE_TAG: {
    title: "Check the baggage tag",
    explanation:
      "No supported border rule makes pickup certain here, but transfer depends on whether the airline tags the bag beyond this airport.",
  },
  US_PRECLEARANCE_NOT_CHECKED_THROUGH: {
    title: "Collect because the bag is not checked through",
    explanation:
      "U.S. border processing was completed before departure, but the baggage tag does not cover the onward flight.",
  },
  US_PRECLEARANCE_CHECKED_THROUGH: {
    title: "Precleared before the U.S. flight",
    explanation:
      "This flight departs from a CBP preclearance airport, so eligible passengers arrive like domestic travellers and a through-checked bag normally transfers onward.",
  },
  US_PRECLEARANCE_CONFIRM_TAG: {
    title: "Preclearance removes the usual U.S. reclaim step",
    explanation:
      "CBP processing happens before departure from this airport. Confirm that the baggage tag covers the onward flight.",
  },
  US_SYD_LAX_SCREENING_PILOT: {
    title: "Collect unless your flight uses the CBP screening pilot",
    explanation:
      "U.S. arrivals normally reclaim checked baggage for CBP. A route-specific remote-screening pilot may transfer eligible bags on the American Airlines Sydney–Los Angeles service.",
  },
  US_FIRST_ARRIVAL: {
    title: "Collect at the first U.S. arrival",
    explanation:
      "Travellers entering the United States from overseas normally collect checked baggage for CBP and recheck it before any onward flight.",
  },
};

const guidance = {
  documentDatasetVersion: snapshot.datasetVersion,
  documents: { rules: { ...documentRules, ...conservativeRules } },
  baggage: { advices: baggageAdvices },
};

await mkdir("src/i18n/guidance", { recursive: true });
await writeFile("src/i18n/guidance/en-GB.json", `${JSON.stringify(guidance, null, 2)}\n`, "utf8");
