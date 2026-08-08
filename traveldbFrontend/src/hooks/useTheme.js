import { useEffect, useState } from "react";

const STORAGE_KEY = "travel-check-theme";

function initialTheme() {
  try {
    const savedTheme = localStorage.getItem(STORAGE_KEY);
    if (savedTheme === "light" || savedTheme === "dark") return savedTheme;
  } catch {
    // Storage can be unavailable in private or restricted browser contexts.
  }
  return "light";
}

export default function useTheme() {
  const [theme, setTheme] = useState(initialTheme);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // Applying the theme should not depend on persistent storage being available.
    }
  }, [theme]);

  function toggleTheme() {
    setTheme(current => current === "light" ? "dark" : "light");
  }

  return { theme, toggleTheme };
}
