import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Search,
  ArrowRight,
  ArrowUpRight,
  Plus,
  ShieldCheck,
  BookOpen,
  HeartHandshake,
  KeyRound,
  Check,
  Package,
} from "lucide-react";
import { useApp } from "../context";
import type { Page } from "../api";
import { ErrorBox, Loading, Empty, useLoad } from "../components/ui";
import { CategoryIcon } from "../components/CategoryIcon";
import { ItemCard } from "../components/ItemCard";
export function Home() {
  const { user, catalog } = useApp();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  return (
    <>
      <section className="hero shell">
        <div className="hero-copy">
          <div className="eyebrow">
            <span className="green-dot" /> GOOD THINGS FIND THEIR WAY BACK
          </div>
          <h1>
            Lost something?
            <br />
            Let’s <span>find it.</span>
          </h1>
          <p>
            Your favourite bottle. Your lecture notes. That one key.
            <br className="desktop" /> Reconnect with the little things that
            matter, right here on campus.
          </p>
          <form
            className="hero-search"
            onSubmit={(e) => {
              e.preventDefault();
              navigate("/browse?q=" + encodeURIComponent(q));
            }}
          >
            <Search size={21} />
            <input
              aria-label="Search for a lost item"
              placeholder="What are you looking for?"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
            <button className="button" type="submit">
              Find it <ArrowRight size={17} />
            </button>
          </form>
          <div className="hero-actions">
            <Link className="button" to="/report/lost">
              I lost something <ArrowUpRight size={17} />
            </Link>
            <Link className="button secondary" to="/report/found">
              I found something <Plus size={17} />
            </Link>
          </div>
          <div className="hero-trust">
            <ShieldCheck size={17} /> A safer way to return things. Built around
            your campus.
          </div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <span className="art-star star-one">✳</span>
          <span className="art-star star-two">✧</span>
          <div className="illustration-note">
            <span className="note-top" />
            <BookOpen size={63} strokeWidth={1.2} />
            <span className="note-line" />
            <span className="note-line short" />
          </div>
          <div className="illustration-bottle">
            <div className="bottle-cap" />
            <div className="bottle-body">
              <HeartHandshake size={34} strokeWidth={1.3} />
            </div>
          </div>
          <div className="illustration-keys">
            <KeyRound size={86} strokeWidth={1.4} />
          </div>
          <div className="floating-tag">
            <span>
              <Check size={15} />
            </span>{" "}
            Back where it belongs.
          </div>
          <div className="art-caption">LESS LOST. MORE FOUND.</div>
        </div>
      </section>
      <div className="community-strip">
        <ShieldCheck size={19} />
        <span>A campus of people looking out for each other.</span>
        <span className="strip-divider" />
        <span>Private claims. Public handovers. Peace of mind.</span>
      </div>
      <section className="shell section">
        <div className="section-heading">
          <div className="eyebrow">A GOOD PLACE TO START</div>
          <h2>What went missing?</h2>
          <p>Big or small, let’s get it back to you.</p>
        </div>
        <div className="categories">
          {catalog.categories.map((c) => (
            <Link
              to={"/browse?categoryId=" + c.id}
              className="category"
              key={c.id}
            >
              <span>
                <CategoryIcon name={c.name} />
              </span>
              <strong>{c.name}</strong>
              <ArrowUpRight size={16} />
            </Link>
          ))}
        </div>
      </section>
      <section className="shell section recent-section">
        <div className="section-top">
          <div>
            <div className="eyebrow">THE CAMPUS NOTICEBOARD</div>
            <h2>A little closer to being found.</h2>
            <p>Recently reported around campus.</p>
          </div>
          <Link className="text-link" to="/browse">
            Browse all items <ArrowRight size={17} />
          </Link>
        </div>
        {user ? (
          <Recent />
        ) : (
          <div className="welcome-card">
            <div className="soft-icon">
              <Package size={32} />
            </div>
            <div>
              <h3>Your campus noticeboard is one sign-in away.</h3>
              <p>
                Sign in to see real reports while keeping campus information
                within the community.
              </p>
            </div>
            <Link to="/login" className="button secondary">
              Sign in to browse <ArrowRight size={17} />
            </Link>
          </div>
        )}
      </section>
      <section className="how-section">
        <div className="shell">
          <div className="section-heading">
            <div className="eyebrow">SMALL STEPS. HAPPY REUNIONS.</div>
            <h2>From “where is it?” to “found it.”</h2>
          </div>
          <div className="steps">
            {[
              [
                "01",
                "Tell us what happened",
                "Lost or found, share a few details. Keep identifying secrets private.",
              ],
              [
                "02",
                "Find a possible match",
                "Search campus reports or explore suggestions based on the details.",
              ],
              [
                "03",
                "Make a safe return",
                "Verify ownership privately and meet at your campus help desk.",
              ],
            ].map(([n, t, d]) => (
              <div key={n}>
                <span className="step-number">{n}</span>
                <h3>{t}</h3>
                <p>{d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
      <section className="shell safety-banner">
        <div className="soft-icon">
          <ShieldCheck size={32} />
        </div>
        <div>
          <h3>A little care goes a long way.</h3>
          <p>
            Keep personal details private. Check ownership. Meet somewhere
            public.
          </p>
        </div>
        <Link to="/safety" className="text-link">
          Our safety guide <ArrowRight size={17} />
        </Link>
      </section>
    </>
  );
}
function Recent() {
  const { data, error, loading } = useLoad<Page>("/reports?size=3");
  return (
    <>
      <ErrorBox error={error} />
      {loading ? (
        <Loading />
      ) : data?.content.length ? (
        <div className="item-grid">
          {data.content.map((i) => (
            <ItemCard key={i.id} item={i} />
          ))}
        </div>
      ) : (
        <Empty>
          No reports yet. Be the first to help someone find their belongings.
        </Empty>
      )}
    </>
  );
}
