import {
  ArrowDown,
  ArrowRight,
  ArrowUp,
  BadgeCheck,
  Check,
  CircleHelp,
  ExternalLink,
  FileText,
  Globe2,
  Info,
  LoaderCircle,
  Luggage,
  MapPin,
  Moon,
  Plane,
  Search,
  ShieldCheck,
  Sun,
  TriangleAlert,
  X,
} from "lucide-react";

const icons = {
  alert: TriangleAlert,
  arrow: ArrowRight,
  down: ArrowDown,
  check: Check,
  close: X,
  document: FileText,
  globe: Globe2,
  info: Info,
  loader: LoaderCircle,
  location: MapPin,
  moon: Moon,
  plane: Plane,
  search: Search,
  shield: ShieldCheck,
  suitcase: Luggage,
  sun: Sun,
  up: ArrowUp,
  verified: BadgeCheck,
  help: CircleHelp,
  external: ExternalLink,
};

export default function Icon({ name, size = 20, strokeWidth = 1.8 }) {
  const LucideIcon = icons[name];
  return (
    <LucideIcon
      aria-hidden="true"
      className={`icon ${name === "loader" ? "is-spinning" : ""}`}
      size={size}
      strokeWidth={strokeWidth}
    />
  );
}
