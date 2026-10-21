import { useState, FormEvent } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ShieldCheck, ArrowRight } from "lucide-react";
import { post, Item, Match } from "../api";
import { useApp } from "../context";
import { ErrorBox, Loading, Empty, useLoad } from "../components/ui";
import { CategoryIcon } from "../components/CategoryIcon";
import { ItemCard } from "../components/ItemCard";
export function Detail() {
  const { id } = useParams();
  const { user } = useApp();
  const { data: r, error, loading, refresh } = useLoad<Item>("/reports/" + id);
  const matches = useLoad<Match[]>("/matches?reportId=" + id);
  const [params] = useSearchParams();
  const [actionError, setActionError] = useState(""),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  async function claim(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    setBusy(true);
    setActionError("");
    try {
      await post("/claims/report/" + id, {
        verification: new FormData(form).get("verification"),
      });
      setMessage(
        "Claim submitted. You can track the review in your dashboard.",
      );
      form.reset();
    } catch (e) {
      setActionError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function close() {
    try {
      await post("/reports/" + id + "/close");
      refresh();
    } catch (e) {
      setActionError((e as Error).message);
    }
  }
  async function flag(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    try {
      await post("/reports/" + id + "/flag", {
        reason: new FormData(e.currentTarget).get("reason"),
      });
      setMessage("Report sent to campus moderators.");
    } catch (e) {
      setActionError((e as Error).message);
    }
  }
  if (loading) return <Loading />;
  if (!r) return <ErrorBox error={error} />;
  const own = r.ownerId === user?.id;
  return (
    <section className="shell section">
      <Link to="/browse" className="text-link">
        ← Back to noticeboard
      </Link>
      <ErrorBox error={error || actionError} />
      {params.has("uploadError") && (
        <div className="error">
          Your report was saved, but the photo upload failed. Edit the report to
          try again.
        </div>
      )}
      {message && (
        <div className="success" role="status">
          {message}
        </div>
      )}
      <div className="detail-grid">
        <div className="detail-image">
          {r.imageUrl ? (
            <img src={r.imageUrl} alt={r.title} />
          ) : (
            <CategoryIcon name={r.category} size={100} />
          )}
        </div>
        <div>
          <span className={"badge " + r.type.toLowerCase()}>
            {r.type} · {r.status}
          </span>
          <h1 className="page-title">{r.title}</h1>
          <p className="detail-description">{r.description}</p>
          <dl>
            <dt>Category</dt>
            <dd>{r.category}</dd>
            <dt>Location</dt>
            <dd>{r.location}</dd>
            <dt>Date {r.type.toLowerCase()}</dt>
            <dd>{r.eventDate}</dd>
            <dt>Colour</dt>
            <dd>{r.colour || "Not specified"}</dd>
          </dl>
          {own && r.status === "ACTIVE" && (
            <div className="hero-actions">
              <Link className="button" to={"/items/" + r.id + "/edit"}>
                Edit report
              </Link>
              <button
                className="button secondary"
                onClick={() => {
                  if (
                    confirm(
                      "Close this report? Pending claims will be rejected.",
                    )
                  )
                    void close();
                }}
              >
                Close report
              </button>
            </div>
          )}
          {!own && r.type === "FOUND" && r.status === "ACTIVE" && (
            <form className="panel form claim-form" onSubmit={claim}>
              <h3>Think this belongs to you?</h3>
              <p>
                Tell the finder something only the owner would know. This stays
                private.
              </p>
              <label>
                Private ownership evidence
                <textarea
                  name="verification"
                  required
                  minLength={10}
                  maxLength={2000}
                  rows={3}
                  placeholder="e.g. a hidden marking or what’s inside"
                />
              </label>
              <button className="button" disabled={busy}>
                {busy ? "Submitting…" : "Submit a claim"}
                <ArrowRight size={17} />
              </button>
            </form>
          )}
          <div className="notice">
            <ShieldCheck size={20} /> Arrange handovers at your campus help
            desk. Never send money to recover an item.
          </div>
        </div>
      </div>
      <h2>Possible matches</h2>
      <p>
        Similarity scores compare details. They are not probabilities of
        ownership.
      </p>
      <ErrorBox error={matches.error} />
      {matches.loading ? (
        <Loading />
      ) : matches.data?.length ? (
        <div className="item-grid">
          {matches.data.map((m) => (
            <div key={m.id}>
              <div className="match-score">
                {m.score}% similarity · {m.explanation}
              </div>
              <ItemCard item={m.lost.id === r.id ? m.found : m.lost} />
            </div>
          ))}
        </div>
      ) : (
        <Empty>No suggestions yet. New reports may help us find a match.</Empty>
      )}
      {!own && (
        <details className="panel flag-box">
          <summary>Something doesn’t look right? Report this listing.</summary>
          <form className="form" onSubmit={flag}>
            <label>
              Reason
              <textarea name="reason" minLength={5} maxLength={1000} required />
            </label>
            <button className="button secondary">Send to moderators</button>
          </form>
        </details>
      )}
    </section>
  );
}
