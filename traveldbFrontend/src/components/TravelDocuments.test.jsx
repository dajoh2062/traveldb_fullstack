import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { createInitialDocumentProfile } from "../utils/journeyForm";
import TravelDocuments from "./TravelDocuments";

const countries = [
  { countryId: "NO", countryNameEn: "Norway" },
  { countryId: "GB", countryNameEn: "United Kingdom" },
  { countryId: "US", countryNameEn: "United States" },
];

function TestDocuments({ errors = {} }) {
  const [documents, setDocuments] = useState(createInitialDocumentProfile);

  return (
    <TravelDocuments
      countries={countries}
      documents={documents}
      errors={errors}
      onChange={(field, value) => setDocuments(current => ({ ...current, [field]: value }))}
    />
  );
}

describe("TravelDocuments", () => {
  it("adds multiple documents and keeps one primary after removal", () => {
    render(<TestDocuments />);

    expect(screen.getAllByRole("combobox", { name: "Document type" })).toHaveLength(1);
    expect(screen.getByRole("radio", { name: "Use document 1 as primary" })).toBeChecked();

    fireEvent.click(screen.getByRole("button", { name: "Add document" }));
    const typeSelects = screen.getAllByRole("combobox", { name: "Document type" });
    expect(typeSelects).toHaveLength(2);
    fireEvent.change(typeSelects[1], { target: { value: "RESIDENCE_PERMIT" } });

    const issuingCountrySelects = screen.getAllByRole("combobox", { name: "Issuing country" });
    fireEvent.change(issuingCountrySelects[1], { target: { value: "GB" } });
    fireEvent.click(screen.getByRole("radio", { name: "Use document 2 as primary" }));

    expect(screen.getByRole("radio", { name: "Use document 1 as primary" })).not.toBeChecked();
    expect(screen.getByRole("radio", { name: "Use document 2 as primary" })).toBeChecked();

    fireEvent.click(screen.getByRole("button", { name: "Remove document 2" }));
    expect(screen.getAllByRole("combobox", { name: "Document type" })).toHaveLength(1);
    expect(screen.getByRole("radio", { name: "Use document 1 as primary" })).toBeChecked();
  });

  it("makes the first document added to an empty list primary", () => {
    render(<TestDocuments />);

    fireEvent.click(screen.getByRole("button", { name: "Remove document 1" }));
    expect(screen.getByText("No documents added yet.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Add document" }));
    expect(screen.getByRole("radio", { name: "Use document 1 as primary" })).toBeChecked();
  });

  it("offers specialist travel documents and a named fallback without document numbers", () => {
    render(<TestDocuments />);

    const typeSelect = screen.getByRole("combobox", { name: "Document type" });
    expect(typeSelect).toHaveTextContent("Refugee travel document (titre de voyage)");
    expect(typeSelect).toHaveTextContent("Travel document for foreigners / alien passport");
    expect(typeSelect).toHaveTextContent("Seafarer's identity document (seaman's book)");

    fireEvent.change(typeSelect, { target: { value: "OTHER" } });
    expect(screen.getByRole("textbox", { name: "Document name" })).toBeInTheDocument();
    expect(screen.queryByLabelText(/document number/i)).not.toBeInTheDocument();
  });

  it("shows list-level and per-document validation errors", () => {
    render(<TestDocuments errors={{
      travelDocuments: {
        _error: "Choose one primary document for this trip.",
        0: { issuingCountryCode: "Select the issuing country." },
      },
    }} />);

    expect(screen.getByText("Choose one primary document for this trip.")).toBeInTheDocument();
    expect(screen.getByText("Select the issuing country.")).toBeInTheDocument();
  });
});
