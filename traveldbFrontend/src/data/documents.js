const documentDetailsByCode = {
  PASSPORT: {
    title: "Valid passport",
    description: "Carry the passport linked to your nationality and travel booking.",
  },
  US_ENTRY_PERMISSION: {
    title: "United States entry permission",
    description: "An approved ESTA, valid visa, or Green Card is required.",
  },
  UK_ETA: {
    title: "United Kingdom ETA",
    description: "An approved Electronic Travel Authorisation is required.",
  },
  AUSTRALIA_ETA: {
    title: "Australia ETA",
    description: "An approved Electronic Travel Authority or appropriate visa is required.",
  },
  NEW_ZEALAND_NZETA: {
    title: "New Zealand NZeTA",
    description: "An approved New Zealand Electronic Travel Authority is required.",
  },
};

export function documentDetails(documentCode) {
  const normalizedCode = documentCode.replace(" (ESTA / VISA / GREENCARD)", "");
  return documentDetailsByCode[normalizedCode] ?? {
    title: documentCode
      .toLowerCase()
      .split("_")
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" "),
    description: "Required for this itinerary.",
  };
}
