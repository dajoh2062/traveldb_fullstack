import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import i18n from "../../i18n";
import { LANGUAGE_STORAGE_KEY } from "../../i18n/locales";
import LanguagePicker from "./LanguagePicker";

beforeEach(async () => {
  localStorage.clear();
  await i18n.changeLanguage("en-GB");
});

describe("LanguagePicker", () => {
  it("shows all supported languages using their native names", () => {
    render(<LanguagePicker />);

    const picker = screen.getByRole("combobox", { name: "Language" });
    expect(picker).toHaveValue("en-GB");
    expect(screen.getAllByRole("option")).toHaveLength(20);
    expect(screen.getByRole("option", { name: "中文（简体）" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Nederlands (België)" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "한국어" })).toBeInTheDocument();
  });

  it("changes and persists the selected language", async () => {
    render(<LanguagePicker />);

    fireEvent.change(screen.getByRole("combobox", { name: "Language" }), {
      target: { value: "ar" },
    });

    await waitFor(() => {
      expect(i18n.resolvedLanguage).toBe("ar");
      expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe("ar");
    });
    expect(screen.getByRole("combobox", { name: "اللغة" })).toHaveValue("ar");
  });
});
