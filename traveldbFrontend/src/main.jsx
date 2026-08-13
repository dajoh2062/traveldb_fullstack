import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./styles/flags.scss";
import "./styles/global.css";
import "./styles/layout.css";
import "./styles/planner.css";
import "./styles/forms.css";
import "./styles/route.css";
import "./styles/results.css";
import "./styles/responsive.css";
import App from "./App";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
