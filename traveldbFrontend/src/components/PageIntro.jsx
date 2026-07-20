import Icon from "./Icon";

export default function PageIntro() {
  return (
    <section className="page-intro" aria-labelledby="page-title">
      <div>
        <span className="overline">Journey checker</span>
        <h1 id="page-title">Check a journey</h1>
        <p>Review baggage collection points and required entry documents before travel.</p>
      </div>
      <div className="check-scope" aria-label="Checks included">
        <span><Icon name="suitcase" size={18} /> Baggage handling</span>
        <span><Icon name="document" size={18} /> Travel documents</span>
      </div>
    </section>
  );
}
