import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import i18n from "../i18n";
import useDocumentLocale from "./useDocumentLocale";

beforeEach(async () => {
  await i18n.changeLanguage("en-GB");
  document.head.innerHTML = '<meta name="description" content="">';
});

describe("useDocumentLocale", () => {
  it("keeps document language, direction and metadata in sync", async () => {
    renderHook(() => useDocumentLocale());

    expect(document.documentElement).toHaveAttribute("lang", "en-GB");
    expect(document.documentElement).toHaveAttribute("dir", "ltr");
    expect(document.title).toBe("TravelDB - Trip requirements");

    await act(() => i18n.changeLanguage("ar"));

    expect(document.documentElement).toHaveAttribute("lang", "ar");
    expect(document.documentElement).toHaveAttribute("dir", "rtl");
    expect(document.title).toBe(i18n.t("meta.title"));
    expect(document.querySelector('meta[name="description"]')).toHaveAttribute(
      "content",
      i18n.t("meta.description"),
    );
  });
});
