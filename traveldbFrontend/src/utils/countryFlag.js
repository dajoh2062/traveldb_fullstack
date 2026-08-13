export function countryFlagCode(countryCode) {
  if (typeof countryCode !== "string") return "";

  const normalizedCode = countryCode.trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(normalizedCode) || normalizedCode === "ZZ") return "";

  return normalizedCode.toLowerCase();
}
