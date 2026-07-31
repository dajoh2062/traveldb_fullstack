export const MAX_TRAVEL_DOCUMENTS = 20;

export const TRAVEL_DOCUMENT_TYPES = [
  { value: "PASSPORT", label: "Passport", issuerRequired: true },
  { value: "DIPLOMATIC_PASSPORT", label: "Diplomatic passport", issuerRequired: true },
  { value: "SERVICE_PASSPORT", label: "Service passport", issuerRequired: true },
  { value: "OFFICIAL_PASSPORT", label: "Official passport", issuerRequired: true },
  { value: "MILITARY_PASSPORT", label: "Military passport", issuerRequired: true },
  {
    value: "ALIEN_PASSPORT",
    label: "Travel document for foreigners / alien passport",
    issuerRequired: true,
  },
  { value: "NATIONAL_ID_CARD", label: "National identity card", issuerRequired: true },
  {
    value: "REFUGEE_TRAVEL_DOCUMENT",
    label: "Refugee travel document (titre de voyage)",
    issuerRequired: true,
  },
  {
    value: "STATELESS_PERSON_TRAVEL_DOCUMENT",
    label: "Stateless person travel document",
    issuerRequired: true,
  },
  {
    value: "EMERGENCY_TRAVEL_DOCUMENT",
    label: "Emergency travel document",
    issuerRequired: true,
  },
  { value: "LAISSEZ_PASSER", label: "Laissez-passer", issuerRequired: false },
  { value: "RESIDENCE_PERMIT", label: "Residence permit", issuerRequired: true },
  { value: "VISA", label: "Visa", issuerRequired: true },
  {
    value: "SEAFARER_IDENTITY_DOCUMENT",
    label: "Seafarer's identity document (seaman's book)",
    issuerRequired: true,
  },
  {
    value: "CREW_MEMBER_CERTIFICATE",
    label: "Crew member certificate",
    issuerRequired: true,
  },
  { value: "MILITARY_ID", label: "Military ID", issuerRequired: true },
  { value: "OTHER", label: "Other travel document", issuerRequired: false },
];

export const PASSPORT_LIKE_DOCUMENT_TYPES = new Set([
  "PASSPORT",
  "DIPLOMATIC_PASSPORT",
  "SERVICE_PASSPORT",
  "OFFICIAL_PASSPORT",
  "MILITARY_PASSPORT",
]);

const DOCUMENT_TYPE_DETAILS = new Map(
  TRAVEL_DOCUMENT_TYPES.map(documentType => [documentType.value, documentType]),
);

let fallbackDocumentId = 0;

function createClientId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();

  fallbackDocumentId += 1;
  return `travel-document-${fallbackDocumentId}`;
}

export function createTravelDocument(overrides = {}) {
  return {
    clientId: overrides.clientId ?? createClientId(),
    type: overrides.type ?? "PASSPORT",
    customType: overrides.customType ?? "",
    issuingCountryCode: overrides.issuingCountryCode ?? "",
    expiryDate: overrides.expiryDate ?? "",
    primary: overrides.primary ?? false,
  };
}

export function getTravelDocumentType(type) {
  return DOCUMENT_TYPE_DETAILS.get(type);
}

export function isPassportLikeDocument(type) {
  return PASSPORT_LIKE_DOCUMENT_TYPES.has(type);
}

export function isOrdinaryPassport(type) {
  return type === "PASSPORT";
}
