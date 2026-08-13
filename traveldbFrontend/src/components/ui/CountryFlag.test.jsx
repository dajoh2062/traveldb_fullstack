import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CountryFlag from "./CountryFlag";

describe("CountryFlag", () => {
  it("normalizes and renders a country code", () => {
    const { container } = render(<CountryFlag countryCode=" NO " />);

    expect(container.firstChild).toHaveClass("country-flag", "fi", "fi-no");
    expect(container.firstChild).toHaveAttribute("data-country-code", "no");
  });

  it.each([undefined, "", "N", "NOR", "ZZ"])(
    "does not render an invalid country code (%s)",
    countryCode => {
      const { container } = render(<CountryFlag countryCode={countryCode} />);
      expect(container).toBeEmptyDOMElement();
    },
  );

  it("supports exceptional two-letter codes supplied by the API", () => {
    const { container } = render(<CountryFlag countryCode="XK" />);
    expect(container.firstChild).toHaveClass("fi-xk");
  });
});
