import { useState, FormEvent } from "react";
import { Link } from "react-router-dom";
import { api, post, Catalog } from "../api";
import { useApp } from "../context";
import { ErrorBox, Loading, Empty, useLoad } from "../components/ui";
export function Admin() {
  const { user, catalog } = useApp();
  const [tab, setTab] = useState("flags"),
    [page, setPage] = useState(0),
    [error, setError] = useState("");
  const stats = useLoad<Record<string, number>>("/admin/stats");
  const list = useLoad<Record<string, unknown>[]>(
    "/admin/" + (tab === "catalog" ? "audit" : tab) + "?page=" + page,
  );
  if (user?.role === "USER")
    return <Empty>You do not have permission to view moderation.</Empty>;
  async function action(url: string) {
    const reason = prompt(
      "Provide a reason for the audit trail (at least 5 characters).",
    );
    if (!reason) return;
    try {
      await post("/admin/" + url, { reason });
      list.refresh();
      stats.refresh();
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function add(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      await post("/admin/catalog/" + f.get("kind"), {
        name: f.get("name"),
        active: true,
      });
      location.reload();
    } catch (e) {
      setError((e as Error).message);
    }
  }
  return (
    <section className="shell section">
      <div className="eyebrow">CAMPUS CARE</div>
      <h1 className="page-title">Moderation workspace</h1>
      <ErrorBox error={error || stats.error || list.error} />
      <div className="stats">
        {stats.data &&
          Object.entries(stats.data).map(([k, v]) => (
            <div className="panel" key={k}>
              <strong>{v}</strong>
              <span>{k.replace(/([A-Z])/g, " $1")}</span>
            </div>
          ))}
      </div>
      <div className="tabs">
        {["flags", "users", "audit", "catalog"].map((t) => (
          <button
            className={tab === t ? "selected" : ""}
            key={t}
            onClick={() => {
              setTab(t);
              setPage(0);
            }}
          >
            {t}
          </button>
        ))}
        <Link to="/dashboard">Review ownership claims →</Link>
      </div>
      {list.loading ? (
        <Loading />
      ) : tab === "catalog" ? (
        <div className="panel form">
          <h3>Manage campus catalog</h3>
          {user?.role === "ADMIN" ? (
            <>
              <form className="form-row" onSubmit={add}>
                <label>
                  Type
                  <select name="kind">
                    <option value="categories">Category</option>
                    <option value="locations">Location</option>
                  </select>
                </label>
                <label>
                  Name
                  <input name="name" required maxLength={120} />
                </label>
                <button className="button">Add</button>
              </form>
              {[
                ["categories", catalog.categories],
                [
                  "locations",
                  catalog.locations.filter((l) => l.campusId === user.campusId),
                ],
              ].map(([kind, entries]) => (
                <div key={String(kind)}>
                  <h3>{String(kind)}</h3>
                  {(entries as Catalog["categories"]).map((c) => (
                    <div className="catalog-row" key={c.id}>
                      {c.name}
                      <button
                        onClick={() => {
                          const name = prompt("New name", c.name);
                          if (name)
                            api("/admin/catalog/" + kind + "/" + c.id, {
                              method: "PUT",
                              body: JSON.stringify({ name, active: true }),
                            })
                              .then(() => location.reload())
                              .catch((e) => setError(e.message));
                        }}
                      >
                        Rename
                      </button>
                      <button
                        onClick={() =>
                          api("/admin/catalog/" + kind + "/" + c.id, {
                            method: "PUT",
                            body: JSON.stringify({
                              name: c.name,
                              active: false,
                            }),
                          })
                            .then(() => location.reload())
                            .catch((e) => setError(e.message))
                        }
                      >
                        Deactivate
                      </button>
                    </div>
                  ))}
                </div>
              ))}
            </>
          ) : (
            <p>Only administrators can edit categories and locations.</p>
          )}
        </div>
      ) : list.data?.length ? (
        list.data.map((row) => (
          <div className="panel moderation-row" key={String(row.id)}>
            {tab === "flags" ? (
              <>
                <Link to={"/items/" + row.reportId}>
                  <h3>{String(row.title)}</h3>
                </Link>
                <p>
                  {String(row.reason)} · {String(row.status)}
                </p>
                <div className="actions">
                  <button
                    onClick={() => action("reports/" + row.reportId + "/hide")}
                  >
                    Hide listing
                  </button>
                  <button onClick={() => action("flags/" + row.id + "/review")}>
                    Mark reviewed
                  </button>
                </div>
              </>
            ) : tab === "users" ? (
              <>
                <h3>{String(row.name)}</h3>
                <p>
                  {String(row.role)} · {String(row.membership)} ·{" "}
                  {row.suspended ? "Suspended" : "Active"}
                </p>
                {user?.role === "ADMIN" && (
                  <div className="actions">
                    <button
                      onClick={() =>
                        action(
                          "users/" +
                            row.id +
                            "/" +
                            (row.suspended ? "restore" : "suspend"),
                        )
                      }
                    >
                      {row.suspended ? "Restore" : "Suspend"}
                    </button>
                    {row.membership === "PENDING" && (
                      <button
                        onClick={() => action("users/" + row.id + "/verify")}
                      >
                        Verify campus membership
                      </button>
                    )}
                  </div>
                )}
              </>
            ) : (
              <>
                <strong>{String(row.action)}</strong>
                <p>
                  {String(row.targetType)} #{String(row.targetId)} ·{" "}
                  {String(row.actor)}
                </p>
                <p>{String(row.reason)}</p>
                <small>
                  {new Date(String(row.createdAt)).toLocaleString()}
                </small>
              </>
            )}
          </div>
        ))
      ) : (
        <Empty>No records to review.</Empty>
      )}
      {tab !== "catalog" && (
        <div className="pagination">
          <button disabled={!page} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span>Page {page + 1}</span>
          <button
            disabled={list.data?.length !== 30}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
