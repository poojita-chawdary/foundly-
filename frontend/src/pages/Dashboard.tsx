import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Bell } from "lucide-react";
import { api, post, Page, Match, Claim } from "../api";
import { useApp } from "../context";
import { ErrorBox, Loading, Empty, useLoad } from "../components/ui";
import { ItemCard } from "../components/ItemCard";
function ClaimCard({
  claim: c,
  refresh,
}: {
  claim: Claim;
  refresh: () => void;
}) {
  const { user } = useApp();
  const [evidence, setEvidence] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function action(status: string) {
    setBusy(true);
    setError("");
    try {
      const handoverMessage =
        status === "APPROVED"
          ? prompt(
              "Suggest a public campus handover place and time. Do not share phone numbers.",
            )
          : undefined;
      if (status === "APPROVED" && !handoverMessage) return;
      await post("/claims/" + c.id + "/decision", { status, handoverMessage });
      refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="panel claim-card">
      <div className="section-top">
        <Link to={"/items/" + c.reportId}>
          <h3>{c.title}</h3>
        </Link>
        <span className="pill">{c.status}</span>
      </div>
      <p>
        Claim by {c.claimantName} · {new Date(c.createdAt).toLocaleDateString()}
      </p>
      <ErrorBox error={error} />
      {c.handoverMessage && (
        <div className="notice">Handover plan: {c.handoverMessage}</div>
      )}
      {c.canReview && (
        <button
          className="text-link"
          onClick={() =>
            api<{ verification: string }>("/claims/" + c.id + "/evidence")
              .then((x) => setEvidence(x.verification))
              .catch((e) => setError(e.message))
          }
        >
          Review private evidence
        </button>
      )}
      {evidence && <div className="private-evidence">{evidence}</div>}
      <div className="actions">
        {c.status === "PENDING" &&
          (c.canReview && c.claimantId !== user?.id ? (
            <>
              <button
                disabled={busy}
                className="button small"
                onClick={() => action("APPROVED")}
              >
                Approve & plan handover
              </button>
              <button
                disabled={busy}
                className="button secondary small"
                onClick={() => action("REJECTED")}
              >
                Reject
              </button>
            </>
          ) : c.claimantId === user?.id ? (
            <button disabled={busy} onClick={() => action("WITHDRAWN")}>
              Withdraw claim
            </button>
          ) : null)}
        {c.canReturn && (
          <button
            className="button small"
            disabled={busy}
            onClick={() => {
              if (confirm("Has the item been safely handed over?"))
                post("/claims/" + c.id + "/returned")
                  .then(refresh)
                  .catch((e) => setError(e.message));
            }}
          >
            Confirm item returned
          </button>
        )}
      </div>
    </div>
  );
}
export function Dashboard() {
  const { user } = useApp();
  const [tab, setTab] = useState("reports"),
    [page, setPage] = useState(0);
  const paths: Record<string, string> = {
    reports: "/reports?mine=true&size=12&page=" + page,
    claims: "/claims?page=" + page,
    review: "/claims?review=true&page=" + page,
    matches: "/matches",
    notifications: "/notifications?page=" + page,
  };
  const { data, error, loading, refresh } = useLoad<unknown>(paths[tab]);
  const reports = tab === "reports" ? (data as Page) : null;
  const claims = ["claims", "review"].includes(tab) ? (data as Claim[]) : null;
  const matches = tab === "matches" ? (data as Match[]) : null;
  const notifications =
    tab === "notifications"
      ? (data as {
          id: number;
          reportId: number;
          message: string;
          read: boolean;
          createdAt: string;
        }[])
      : null;
  return (
    <section className="shell section">
      <div className="section-top">
        <div>
          <div className="eyebrow">YOUR LITTLE CORNER OF CAMPUS</div>
          <h1 className="page-title">Hi, {user?.name.split(" ")[0]}.</h1>
          <p>Reports, possible matches, and happy reunions.</p>
        </div>
        <Link to="/report/lost" className="button">
          <Plus size={17} /> New report
        </Link>
      </div>
      <div className="tabs">
        {[
          ["reports", "My reports"],
          ["matches", "Potential matches"],
          ["claims", "My claims"],
          ["review", "Claims to review"],
          ["notifications", "Notifications"],
        ].map(([key, title]) => (
          <button
            key={key}
            className={tab === key ? "selected" : ""}
            onClick={() => {
              setTab(key);
              setPage(0);
            }}
          >
            {key === "notifications" && <Bell size={15} />} {title}
          </button>
        ))}
      </div>
      <ErrorBox error={error} />
      {loading ? (
        <Loading />
      ) : (
        <>
          {reports &&
            (reports.content.length ? (
              <div className="item-grid">
                {reports.content.map((i) => (
                  <ItemCard key={i.id} item={i} />
                ))}
              </div>
            ) : (
              <Empty>
                Your reports will appear here.{" "}
                <Link to="/report/lost">Create your first report</Link>.
              </Empty>
            ))}
          {claims &&
            (claims.length ? (
              claims.map((c) => (
                <ClaimCard key={c.id} claim={c} refresh={refresh} />
              ))
            ) : (
              <Empty>No claims here yet.</Empty>
            ))}
          {matches &&
            (matches.length ? (
              <div className="item-grid">
                {matches.map((m) => (
                  <div key={m.id}>
                    <div className="match-score">
                      {m.score}% similarity<p>{m.explanation}</p>
                    </div>
                    <ItemCard
                      item={m.lost.ownerId === user?.id ? m.found : m.lost}
                    />
                    <button
                      onClick={() =>
                        post("/matches/" + m.id + "/dismiss")
                          .then(refresh)
                          .catch((e) => alert(e.message))
                      }
                    >
                      Dismiss suggestion
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <Empty>
                No suggestions yet. We’ll notify you when a possible match
                appears.
              </Empty>
            ))}
          {matches && (
            <p className="muted">
              Similarity compares item details. It does not prove ownership.
            </p>
          )}
          {notifications &&
            (notifications.length ? (
              notifications.map((n) => (
                <div
                  className={"panel notification " + (!n.read ? "unread" : "")}
                  key={n.id}
                >
                  <Link to={"/items/" + n.reportId}>{n.message}</Link>
                  <small>{new Date(n.createdAt).toLocaleString()}</small>
                  {!n.read && (
                    <button
                      onClick={() =>
                        post("/notifications/" + n.id + "/read")
                          .then(refresh)
                          .catch((e) => alert(e.message))
                      }
                    >
                      Mark read
                    </button>
                  )}
                </div>
              ))
            ) : (
              <Empty>You’re all caught up.</Empty>
            ))}
        </>
      )}
      {tab !== "matches" && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span>Page {page + 1}</span>
          <button
            disabled={
              loading ||
              (reports
                ? reports.total <= (page + 1) * 12
                : (data as unknown[] | null)?.length !== 30)
            }
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
