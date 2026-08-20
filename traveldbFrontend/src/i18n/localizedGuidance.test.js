import { beforeEach, describe, expect, it } from "vitest";
import i18n from ".";
import {
  localizeBaggageAdvice,
  localizeDocumentRequirement,
} from "./localizedGuidance";

const CURRENT_DATASET = "2026-08-09.5";

beforeEach(async () => {
  await i18n.changeLanguage("en-GB");
});

describe("localized guidance", () => {
  it("uses a unique document rule id instead of the shared output code", () => {
    const localized = localizeDocumentRequirement(
      {
        ruleId: "japan-thailand-epassport-waiver-15",
        code: "JAPAN_SHORT_STAY_PERMISSION",
        title: "backend title",
        summary: "backend summary",
        conditions: ["backend condition"],
        keyFacts: [{ label: "backend label", value: "backend value" }],
      },
      i18n,
      CURRENT_DATASET,
    );

    expect(localized.localized.title.text).toBe("Visa-free only with a Thai ePassport");
    expect(localized.localized.summary.text).not.toBe("backend summary");
    expect(localized.localized.conditions[0].text).not.toBe("backend condition");
    expect(localized.localized.keyFacts[0].value.text).not.toBe("backend value");
  });

  it("falls back to marked English for unknown, changed, or stale rules", async () => {
    await i18n.changeLanguage("fr");
    const unknown = localizeDocumentRequirement(
      { ruleId: "future-rule", title: "Future rule", conditions: [], keyFacts: [] },
      i18n,
      CURRENT_DATASET,
    );
    const changed = localizeDocumentRequirement(
      {
        ruleId: "japan-thailand-epassport-waiver-15",
        title: "Updated English title",
        conditions: [],
        keyFacts: [],
      },
      i18n,
      "future-dataset",
    );
    const stale = localizeDocumentRequirement(
      {
        ruleId: "japan-thailand-epassport-waiver-15",
        title: "English review warning",
        reviewAfter: "2020-01-01",
        conditions: [],
        keyFacts: [],
      },
      i18n,
      CURRENT_DATASET,
    );

    expect(unknown.localized.title).toEqual({
      text: "Future rule",
      lang: "en",
      isLocalized: false,
    });
    expect(changed.localized.title.text).toBe("Updated English title");
    expect(stale.localized.title.text).toBe("English review warning");
    expect(stale.isFullyLocalized).toBe(false);
  });

  it("keeps UK and US baggage wording distinct", async () => {
    const advice = {
      adviceCode: "US_FIRST_ARRIVAL",
      title: "backend title",
      explanation: "backend explanation",
    };
    const british = localizeBaggageAdvice(advice, i18n);

    await i18n.changeLanguage("en-US");
    const american = localizeBaggageAdvice(advice, i18n);

    expect(british.localized.explanation.text).toContain("Travellers");
    expect(american.localized.explanation.text).toContain("Travelers");
  });
});
