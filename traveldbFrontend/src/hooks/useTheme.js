import { useEffect, useState } from "react";

export default function useTheme() {
  const [theme, setTheme] = useState(() => localStorage.getItem("travel-check-theme") || "light");

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("travel-check-theme", theme);
  }, [theme]);

  function toggleTheme() {
    setTheme(current => current === "light" ? "dark" : "light");
  }

  return { theme, toggleTheme };
}
