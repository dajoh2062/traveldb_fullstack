import { describe, expect, it } from "vitest";
import { countryFlagCode } from "./countryFlag";

describe("countryFlagCode", () => {
  it("normalizes ISO alpha-2 codes for bundled SVG flags", () => {
    expect(countryFlagCode(" no ")).toBe("no");
    expect(countryFlagCode("GB")).toBe("gb");
  });

  it("does not render a misleading flag for missing or invalid codes", () => {
    expect(countryFlagCode()).toBe("");
    expect(countryFlagCode("NOR")).toBe("");
    expect(countryFlagCode("1A")).toBe("");
    expect(countryFlagCode("ZZ")).toBe("");
    expect(countryFlagCode("XK")).toBe("xk");
  });
});
