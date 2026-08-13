import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach } from "vitest";
import i18n from "../i18n";

beforeEach(async () => {
  await i18n.changeLanguage("en-GB");
});

afterEach(async () => {
  cleanup();
  await i18n.changeLanguage("en-GB");
  localStorage.removeItem("traveldb-language");
  document.documentElement.lang = "en-GB";
  document.documentElement.dir = "ltr";
});
