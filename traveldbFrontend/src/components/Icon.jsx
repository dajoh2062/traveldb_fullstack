import {
  ArrowDown,
  ArrowRight,
  ArrowUp,
  Check,
  CircleHelp,
  ExternalLink,
  FileText,
  Globe2,
  LoaderCircle,
  Luggage,
  Moon,
  Search,
  Sun,
  TriangleAlert,
  X,
} from "lucide-react";

const ICONS = {
  alert: TriangleAlert,
  arrow: ArrowRight,
  down: ArrowDown,
  check: Check,
  close: X,
  document: FileText,
  globe: Globe2,
  loader: LoaderCircle,
  moon: Moon,
  search: Search,
  suitcase: Luggage,
  sun: Sun,
  up: ArrowUp,
  help: CircleHelp,
  external: ExternalLink,
};

export default function Icon({ name, size = 20, strokeWidth = 1.8 }) {
  const LucideIcon = ICONS[name];
  return (
    <LucideIcon
      aria-hidden="true"
      className={`icon ${name === "loader" ? "is-spinning" : ""}`}
      size={size}
      strokeWidth={strokeWidth}
    />
  );
}
