import Icon from "./Icon";

export default function AppHeader({ theme, onToggleTheme }) {
  return (
    <header className="topbar">
      <a className="brand" href="#top" aria-label="TravelDB home">
        <span className="brand-mark"><Icon name="plane" size={23} strokeWidth={2} /></span>
        <span>TravelDB</span>
      </a>
      <div className="topbar-actions">
        <button
          aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
          className="theme-toggle"
          onClick={onToggleTheme}
          type="button"
        >
          <Icon name={theme === "light" ? "moon" : "sun"} size={18} />
        </button>
      </div>
    </header>
  );
}
