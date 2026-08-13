import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import AppHeader from "./AppHeader";

describe("AppHeader", () => {
  it("exposes translated labels for navigation and controls", () => {
    render(<AppHeader onToggleTheme={vi.fn()} theme="light" />);

    expect(screen.getByRole("link", { name: "TravelDB home" })).toHaveAttribute("href", "#top");
    expect(screen.getByRole("combobox", { name: "Language" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Switch to dark mode" })).toBeInTheDocument();
  });
});
