function normalizeCountryCode(countryCode) {
  if (typeof countryCode !== "string") return null;

  const normalizedCode = countryCode.trim().toLowerCase();
  if (!/^[a-z]{2}$/.test(normalizedCode) || normalizedCode === "zz") return null;

  return normalizedCode;
}

export default function CountryFlag({ countryCode }) {
  const flagCode = normalizeCountryCode(countryCode);
  if (!flagCode) return null;

  return (
    <span
      aria-hidden="true"
      className={`country-flag fi fi-${flagCode}`}
      data-country-code={flagCode}
    />
  );
}
