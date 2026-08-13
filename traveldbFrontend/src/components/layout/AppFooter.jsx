import { useTranslation } from "react-i18next";

export default function AppFooter() {
  const { t } = useTranslation();

  return (
    <footer>
      <p>{t("footer.disclaimer")}</p>
    </footer>
  );
}
