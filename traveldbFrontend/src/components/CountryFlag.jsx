import { countryFlagCode } from "../utils/countryFlag";

export default function CountryFlag({ countryCode }) {
  const flagCode = countryFlagCode(countryCode);
  if (!flagCode) return null;

  const flagUrl = new URL(
    `../../node_modules/flag-icons/flags/4x3/${flagCode}.svg`,
    import.meta.url,
  ).href;

  return (
    <span
      aria-hidden="true"
      className="country-flag"
      data-country-code={flagCode}
      style={{ backgroundImage: `url("${flagUrl}")` }}
    />
  );
}
