import { useTranslation } from "react-i18next";

export default function PageIntro() {
  const { t } = useTranslation();

  return (
    <section className="page-intro" aria-labelledby="page-title">
      <div>
        <p className="page-kicker">{t("intro.kicker")}</p>
        <h1 id="page-title">{t("intro.title")}</h1>
      </div>
      <p className="page-intro-copy">{t("intro.description")}</p>
    </section>
  );
}
