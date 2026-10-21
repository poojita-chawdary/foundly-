import { FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { SlidersHorizontal } from "lucide-react";
import { useApp } from "../context";
import type { Page } from "../api";
import { ErrorBox, Loading, Empty, useLoad } from "../components/ui";
import { ItemCard } from "../components/ItemCard";
export function Browse() {
  const { catalog, user } = useApp();
  const [params, setParams] = useSearchParams();
  const { data, error, loading } = useLoad<Page>(
    "/reports?" + params.toString(),
  );
  function filter(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const next = new URLSearchParams();
    form.forEach((v, k) => {
      if (v) next.set(k, String(v));
    });
    setParams(next);
  }
  function page(n: number) {
    const p = new URLSearchParams(params);
    p.set("page", String(n));
    setParams(p);
  }
  return (
    <section className="shell section">
      <div className="eyebrow">YOUR CAMPUS NOTICEBOARD</div>
      <h1 className="page-title">Let’s find what you’re looking for.</h1>
      <p>
        Browse active reports. A familiar detail could be the missing piece.
      </p>
      <form
        className="filter-bar panel"
        onSubmit={filter}
        key={[...params].filter(([k]) => k !== "page").toString()}
      >
        <label className="search-field">
          Search
          <input
            name="q"
            placeholder="Try “blue bottle”"
            defaultValue={params.get("q") ?? ""}
          />
        </label>
        <label>
          Report type
          <select name="type" defaultValue={params.get("type") ?? ""}>
            <option value="">Lost & found</option>
            <option>LOST</option>
            <option>FOUND</option>
          </select>
        </label>
        <label>
          Category
          <select
            name="categoryId"
            defaultValue={params.get("categoryId") ?? ""}
          >
            <option value="">All categories</option>
            {catalog.categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Location
          <select
            name="locationId"
            defaultValue={params.get("locationId") ?? ""}
          >
            <option value="">Everywhere</option>
            {catalog.locations
              .filter((c) => c.campusId === user?.campusId)
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
          </select>
        </label>
        <label>
          From
          <input
            type="date"
            name="from"
            defaultValue={params.get("from") ?? ""}
          />
        </label>
        <label>
          To
          <input type="date" name="to" defaultValue={params.get("to") ?? ""} />
        </label>
        <button className="button">
          <SlidersHorizontal size={17} /> Apply
        </button>
      </form>
      <ErrorBox error={error} />
      {loading ? (
        <Loading />
      ) : (
        <>
          <div className="results-label">
            {data?.total ?? 0} reports · newest first
          </div>
          {data?.content.length ? (
            <div className="item-grid">
              {data.content.map((i) => (
                <ItemCard key={i.id} item={i} />
              ))}
            </div>
          ) : (
            <Empty>
              No reports match these filters. Try a broader search or{" "}
              <Link to="/report/lost">report a lost item</Link>.
            </Empty>
          )}
          <div className="pagination">
            <button
              disabled={!data || data.page === 0}
              onClick={() => page((data?.page ?? 0) - 1)}
            >
              Previous
            </button>
            <span>Page {(data?.page ?? 0) + 1}</span>
            <button
              disabled={!data || (data.page + 1) * data.size >= data.total}
              onClick={() => page((data?.page ?? 0) + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </section>
  );
}
